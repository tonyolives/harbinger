package com.harbinger.service.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.harbinger.llm.MockLlmProvider;
import com.harbinger.model.Lead;
import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import com.harbinger.model.Tier;
import com.harbinger.repository.InMemoryLeadRepository;
import com.harbinger.repository.LeadRepository;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import com.harbinger.service.resolution.ResolutionService;
import com.harbinger.service.scoring.ScoringService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves the realtime-loop behavior: a homeowner crossing HOT surfaces exactly one lead and
 * fires exactly one publish (no re-alert on later signals), the north-star {@code signalToLeadMs}
 * is measured against the orchestrator's clock, the surfaced lead carries the LLM explanation,
 * and leads come back ranked. Scoring uses its own fixed clock so it never consumes the
 * orchestrator's stepping clock.
 */
class RealtimeLeadServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-06T12:00:00Z");
    private static final long STEP_MS = 5;

    /** A clock that advances {@link #STEP_MS} on every {@code instant()} — deterministic latency. */
    private static final class SteppingClock extends Clock {
        private int calls = 0;
        @Override public Instant instant() {
            return NOW.plusMillis(STEP_MS * calls++);
        }
        @Override public ZoneId getZone() {
            return ZoneOffset.UTC;
        }
        @Override public Clock withZone(ZoneId zone) {
            return this;
        }
    }

    private final LeadRepository repository = new InMemoryLeadRepository();
    private final LeadEventPublisher publisher = mock(LeadEventPublisher.class);
    private final SignalPipeline pipeline = new SignalPipeline(
            new ResolutionService(new NameNormalizer(), new AddressNormalizer()),
            new ScoringService(Clock.fixed(NOW, ZoneOffset.UTC)));
    private final RealtimeLeadService service = new RealtimeLeadService(
            pipeline, new MockLlmProvider(), repository, publisher, new SteppingClock());

    private static RawSignal signal(String name, String address, SignalType type) {
        return new RawSignal(name, address, Source.COUNTY_RECORDER, type, NOW, UUID.randomUUID());
    }

    @Test
    void crossingHotSurfacesExactlyOneLeadAndAlertsOnce() {
        // Below threshold first, then crosses HOT, then more signals while already HOT.
        assertThat(service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE)))
                .isEmpty(); // 45 → WARM, no surface

        Optional<Lead> surfaced =
                service.ingest(signal("J. Smith", "123 Main St", SignalType.TAX_DELINQUENCY));
        assertThat(surfaced).isPresent(); // 75 → HOT, surfaces

        // Already HOT: more signals must not re-alert.
        assertThat(service.ingest(signal("John Smith", "123 Main St", SignalType.EVICTION)))
                .isEmpty();

        verify(publisher, times(1)).publish(surfaced.orElseThrow());
        assertThat(repository.count()).isEqualTo(1);
        assertThat(service.leadsSurfaced()).isEqualTo(1);
        assertThat(service.signalsProcessed()).isEqualTo(3);
    }

    @Test
    void surfacedLeadIsHotCarriesExplanationAndMeasuredLatency() {
        service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE));
        Lead lead = service.ingest(signal("John Smith", "123 Main St", SignalType.TAX_DELINQUENCY))
                .orElseThrow();

        assertThat(lead.tier()).isEqualTo(Tier.HOT);
        assertThat(lead.intentScore()).isEqualTo(75);
        assertThat(lead.explanation()).isNotBlank().contains("Pre-foreclosure filing");
        // Within one ingest the stepping clock advances once: arrival → surface = STEP_MS.
        assertThat(lead.signalToLeadMs()).isEqualTo(STEP_MS);
    }

    @Test
    void alreadyHotLeadIsRefreshedInPlaceWithoutReAlerting() {
        service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE));
        Lead first = service.ingest(signal("John Smith", "123 Main St", SignalType.TAX_DELINQUENCY))
                .orElseThrow();
        // A further signal raises the score; the stored lead updates but doesn't re-alert.
        service.ingest(signal("John Smith", "123 Main St", SignalType.PROBATE));

        Lead refreshed = repository.findById(first.homeowner().id()).orElseThrow();
        assertThat(refreshed.intentScore()).isGreaterThan(first.intentScore());
        assertThat(refreshed.surfacedAt()).isEqualTo(first.surfacedAt()); // kept from first crossing
        verify(publisher, times(1)).publish(first);
    }

    @Test
    void leadsComeBackRanked() {
        // Owner B reaches 80, owner A reaches 75 → B ranks first.
        service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE));
        service.ingest(signal("John Smith", "123 Main St", SignalType.TAX_DELINQUENCY)); // A = 75 HOT
        service.ingest(signal("Dana Jones", "9 Oak Ave", SignalType.PROBATE));
        service.ingest(signal("Dana Jones", "9 Oak Ave", SignalType.PRE_FORECLOSURE)); // B = 80 HOT

        List<Lead> ranked = repository.findAllRanked();
        assertThat(ranked).hasSize(2);
        assertThat(ranked.get(0).intentScore()).isEqualTo(80);
        assertThat(ranked.get(1).intentScore()).isEqualTo(75);
    }

    @Test
    void nullSignalRejected() {
        assertThatThrownBy(() -> service.ingest(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
