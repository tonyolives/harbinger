package com.harbinger.service.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves the realtime-loop behavior: every homeowner becomes a live lead row on its first signal
 * (at whatever tier), rows update in place as the score climbs with the explanation kept in sync,
 * unchanged homeowners aren't re-published, the north-star {@code signalToLeadMs} is measured at
 * surfacing, and leads come back ranked. Scoring uses its own fixed clock so it never consumes the
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
    void firstSignalCreatesARowAtItsTierAndPublishesOnce() {
        List<Lead> touched = service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE));

        assertThat(touched).hasSize(1);
        assertThat(touched.get(0).tier()).isEqualTo(Tier.WARM); // 45 → WARM, still a row
        assertThat(repository.count()).isEqualTo(1);
        assertThat(service.leadsSurfaced()).isEqualTo(1);
        verify(publisher, times(1)).publish(any(Lead.class));
    }

    @Test
    void crossingToHotUpdatesTheSameRowAndRepublishes() {
        service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE)); // WARM 45
        List<Lead> touched =
                service.ingest(signal("J. Smith", "123 Main St", SignalType.TAX_DELINQUENCY)); // → HOT 75

        assertThat(touched).hasSize(1);
        assertThat(repository.count()).isEqualTo(1); // same homeowner, updated in place
        Lead lead = repository.findById(touched.get(0).homeowner().id()).orElseThrow();
        assertThat(lead.tier()).isEqualTo(Tier.HOT);
        assertThat(lead.intentScore()).isEqualTo(75);
        verify(publisher, times(2)).publish(any(Lead.class)); // create + update
    }

    @Test
    void explanationStaysConsistentWithTheUpdatedScore() {
        service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE)); // WARM 45
        service.ingest(signal("John Smith", "123 Main St", SignalType.TAX_DELINQUENCY)); // HOT 75

        Lead lead = repository.findAllRanked().get(0);
        // The explanation must reflect the *current* score/tier, not the surfacing-time text.
        assertThat(lead.explanation()).contains("HOT").contains("score 75");
    }

    @Test
    void unchangedHomeownerIsNotRepublished() {
        Lead leadA = service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE)).get(0);
        // A different homeowner arrives; A is re-scored but unchanged, so it must not republish.
        service.ingest(signal("Dana Jones", "9 Oak Ave", SignalType.EVICTION));

        verify(publisher, times(1)).publish(leadA);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void leadsComeBackRanked() {
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
    void signalToLeadMsMeasuredAtSurfacing() {
        Lead lead = service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE)).get(0);
        // Within one ingest the stepping clock advances once: arrival → surface = STEP_MS.
        assertThat(lead.signalToLeadMs()).isEqualTo(STEP_MS);
    }

    @Test
    void consolidatesWhenAHomeownerIdDriftsToAFullerName() {
        // An initial-only name, then the full name at the same address: the resolved id changes,
        // so the tentative earlier row is pruned and exactly one lead remains.
        service.ingest(signal("J Pollich", "61338 Labadie Manor", SignalType.PROBATE));
        service.ingest(signal("Jean Pollich", "61338 Labadie Manor", SignalType.PRE_FORECLOSURE));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findAllRanked().get(0).homeowner().name()).isEqualTo("jean pollich");
    }

    @Test
    void resetClearsAllLiveState() {
        service.ingest(signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE));
        service.ingest(signal("Dana Jones", "9 Oak Ave", SignalType.PROBATE));
        assertThat(service.signalsProcessed()).isEqualTo(2);
        assertThat(service.leadsSurfaced()).isEqualTo(2);

        service.reset();

        assertThat(service.signalsProcessed()).isZero();
        assertThat(service.leadsSurfaced()).isZero();
        assertThat(service.tierCounts()).isEmpty();
        assertThat(repository.findAllRanked()).isEmpty();
    }

    @Test
    void nullSignalRejected() {
        assertThatThrownBy(() -> service.ingest(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
