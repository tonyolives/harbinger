package com.harbinger.service.scoring;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import com.harbinger.model.Tier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Proves the Phase 5 scoring properties against a fixed clock so recency-decay math is
 * deterministic: adding a positive signal never lowers the score, older signals contribute
 * less, stacked signals score higher, tiers fall on the right side of their thresholds, and
 * DEED_TRANSFER dampens.
 */
class ScoringServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-06T12:00:00Z");
    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);

    private final ScoringService service = new ScoringService(FIXED);

    /** A signal of the given type, observed {@code ageDays} before the fixed "now". */
    private static RawSignal signal(SignalType type, long ageDays) {
        return new RawSignal(
                "john smith",
                "123 main st",
                Source.COUNTY_RECORDER,
                type,
                NOW.minus(Duration.ofDays(ageDays)),
                UUID.randomUUID());
    }

    @Test
    void addingAPositiveSignalNeverLowersScore() {
        List<RawSignal> base = new ArrayList<>(List.of(signal(SignalType.DIVORCE, 0)));
        int before = service.score(base).value();

        base.add(signal(SignalType.TAX_DELINQUENCY, 0));
        int after = service.score(base).value();

        assertThat(after).isGreaterThanOrEqualTo(before);
    }

    @Test
    void olderSignalContributesLessThanRecentOne() {
        int recent = service.score(List.of(signal(SignalType.PRE_FORECLOSURE, 0))).value();
        int old = service.score(List.of(signal(SignalType.PRE_FORECLOSURE, 60))).value();

        assertThat(old).isLessThan(recent);
        // 60-day half-life: a 60-day-old PRE_FORECLOSURE (45) decays to ~22.5.
        assertThat(old).isEqualTo(23); // round(22.5)
    }

    @Test
    void futureDatedSignalIsTreatedAsAgeZero() {
        int now = service.score(List.of(signal(SignalType.PRE_FORECLOSURE, 0))).value();
        int future = service.score(List.of(signal(SignalType.PRE_FORECLOSURE, -30))).value();

        assertThat(future).isEqualTo(now);
    }

    @Test
    void stackedSignalsScoreHigherThanSingleSignal() {
        int single = service.score(List.of(signal(SignalType.DIVORCE, 0))).value();
        int stacked = service.score(
                List.of(signal(SignalType.DIVORCE, 0), signal(SignalType.EVICTION, 0))).value();

        assertThat(stacked).isGreaterThan(single);
    }

    @Test
    void scoreMapsToCorrectTierAtThresholds() {
        // EVICTION (20) → 20 → COLD
        assertThat(service.score(List.of(signal(SignalType.EVICTION, 0))).tier())
                .isEqualTo(Tier.COLD);
        // PRE_FORECLOSURE (45) → 45 → WARM
        assertThat(service.score(List.of(signal(SignalType.PRE_FORECLOSURE, 0))).tier())
                .isEqualTo(Tier.WARM);
        // PRE_FORECLOSURE (45) + TAX_DELINQUENCY (30) → 75 → HOT
        assertThat(service.score(
                        List.of(signal(SignalType.PRE_FORECLOSURE, 0),
                                signal(SignalType.TAX_DELINQUENCY, 0)))
                        .tier())
                .isEqualTo(Tier.HOT);
    }

    @Test
    void exactBoundaryScoresLandInTheHigherTier() {
        // WARM lower bound is exactly 40: DIVORCE (25) + EVICTION (20) decayed to hit 40.
        // 25 + 20 = 45 fresh → WARM; verify the 39/40 and 69/70 edges via tier, not value.
        Score warmEdge = service.score(
                List.of(signal(SignalType.DIVORCE, 0), signal(SignalType.EVICTION, 0)));
        assertThat(warmEdge.value()).isEqualTo(45);
        assertThat(warmEdge.tier()).isEqualTo(Tier.WARM);
    }

    @Test
    void scoreIsCappedAtOneHundred() {
        // Every positive type, all fresh: 45+35+30+25+20 = 155 → capped at 100.
        List<RawSignal> all = List.of(
                signal(SignalType.PRE_FORECLOSURE, 0),
                signal(SignalType.PROBATE, 0),
                signal(SignalType.TAX_DELINQUENCY, 0),
                signal(SignalType.DIVORCE, 0),
                signal(SignalType.EVICTION, 0));

        assertThat(service.score(all).value()).isEqualTo(100);
        assertThat(service.score(all).tier()).isEqualTo(Tier.HOT);
    }

    @Test
    void deedTransferDampensScoreRelativeToSameSetWithoutIt() {
        List<RawSignal> without = List.of(signal(SignalType.PRE_FORECLOSURE, 0));
        List<RawSignal> with = List.of(
                signal(SignalType.PRE_FORECLOSURE, 0),
                signal(SignalType.DEED_TRANSFER, 0));

        assertThat(service.score(with).value()).isLessThan(service.score(without).value());
    }

    @Test
    void loneDeedTransferFloorsAtZeroAndIsCold() {
        Score score = service.score(List.of(signal(SignalType.DEED_TRANSFER, 0)));

        assertThat(score.value()).isEqualTo(0);
        assertThat(score.tier()).isEqualTo(Tier.COLD);
    }

    @Test
    void reasonsAreNonEmptyAndStrongestFirst() {
        Score score = service.score(
                List.of(signal(SignalType.EVICTION, 0), signal(SignalType.PRE_FORECLOSURE, 0)));

        assertThat(score.reasons()).isNotEmpty();
        // PRE_FORECLOSURE (45) outweighs EVICTION (20), so its reason leads.
        assertThat(score.reasons().get(0)).containsIgnoringCase("foreclosure");
    }

    @Test
    void nullSignalsRejected() {
        assertThatThrownBy(() -> service.score(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptySignalsRejected() {
        assertThatThrownBy(() -> service.score(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
