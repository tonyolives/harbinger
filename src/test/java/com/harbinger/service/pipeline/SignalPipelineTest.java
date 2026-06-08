package com.harbinger.service.pipeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import com.harbinger.model.Tier;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import com.harbinger.service.resolution.ResolutionService;
import com.harbinger.service.scoring.ScoringService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves the resolve → score composition: messy signals collapse to one scored homeowner per
 * person, with the tier scoring would assign. Uses real (pure) services against a fixed clock.
 */
class SignalPipelineTest {

    private static final Instant NOW = Instant.parse("2026-06-06T12:00:00Z");

    private final SignalPipeline pipeline = new SignalPipeline(
            new ResolutionService(new NameNormalizer(), new AddressNormalizer()),
            new ScoringService(Clock.fixed(NOW, ZoneOffset.UTC)));

    private static RawSignal signal(String name, String address, SignalType type) {
        return new RawSignal(name, address, Source.COUNTY_RECORDER, type, NOW, UUID.randomUUID());
    }

    @Test
    void resolvesAndScoresOneHomeownerPerPerson() {
        // Owner A: PRE_FORECLOSURE (45) + TAX_DELINQUENCY (30) = 75 → HOT, across messy variants.
        // Owner B: a lone EVICTION (20) → COLD.
        List<RawSignal> signals = List.of(
                signal("John Smith", "123 Main St", SignalType.PRE_FORECLOSURE),
                signal("J. Smith", "123 Main Street", SignalType.TAX_DELINQUENCY),
                signal("Dana Jones", "9 Oak Ave", SignalType.EVICTION));

        List<ScoredHomeowner> scored = pipeline.score(signals);

        assertThat(scored).hasSize(2);
        ScoredHomeowner hot = scored.stream()
                .filter(s -> s.score().tier() == Tier.HOT).findFirst().orElseThrow();
        ScoredHomeowner cold = scored.stream()
                .filter(s -> s.score().tier() == Tier.COLD).findFirst().orElseThrow();
        assertThat(hot.score().value()).isEqualTo(75);
        assertThat(hot.homeowner().address()).isEqualTo("123 main st");
        assertThat(cold.score().value()).isEqualTo(20);
    }

    @Test
    void emptySignalsYieldNoHomeowners() {
        assertThat(pipeline.score(List.of())).isEmpty();
    }

    @Test
    void nullSignalsRejected() {
        assertThatThrownBy(() -> pipeline.score(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
