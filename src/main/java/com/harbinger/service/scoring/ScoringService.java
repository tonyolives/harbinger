package com.harbinger.service.scoring;

import com.harbinger.model.RawSignal;
import com.harbinger.model.SignalType;
import com.harbinger.model.Tier;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Phase 5: turns a homeowner's {@link RawSignal}s into an intent {@link Score} — a value in
 * [0, 100], a {@link Tier}, and human-readable reasons. The model is a rules-based v1 (no
 * trained model; the weights below are an explainable prior). See {@code docs/SCORING.md}.
 *
 * <p>The computation is pure: a weighted sum of signals where each signal's weight is decayed
 * by how old it is, then floored at 0 and capped at 100. The one source of "now" is the
 * injected {@link Clock}, so recency is deterministic and unit-testable — no {@code Instant.now()}.
 *
 * <pre>
 *   decay = 0.5 ^ (ageDays / HALF_LIFE_DAYS)        // older signals count less
 *   raw   = Σ weight(signalType) · decay            // DEED_TRANSFER's weight is negative
 *   value = round(clamp(0, 100, raw))
 * </pre>
 */
@Service
public class ScoringService {

    /** Per-type weights, all in one place so the model is auditable. Mirrors {@code docs/SCORING.md}. */
    private static final Map<SignalType, Double> WEIGHTS = new EnumMap<>(SignalType.class);

    /** Plain-English labels for the reasons list. */
    private static final Map<SignalType, String> LABELS = new EnumMap<>(SignalType.class);

    static {
        WEIGHTS.put(SignalType.PRE_FORECLOSURE, 45.0);
        WEIGHTS.put(SignalType.PROBATE, 35.0);
        WEIGHTS.put(SignalType.TAX_DELINQUENCY, 30.0);
        WEIGHTS.put(SignalType.DIVORCE, 25.0);
        WEIGHTS.put(SignalType.EVICTION, 20.0);
        // Negative: a home that just changed hands is less likely to be on the market.
        WEIGHTS.put(SignalType.DEED_TRANSFER, -30.0);

        LABELS.put(SignalType.PRE_FORECLOSURE, "Pre-foreclosure filing");
        LABELS.put(SignalType.PROBATE, "Probate (inherited property)");
        LABELS.put(SignalType.TAX_DELINQUENCY, "Tax delinquency");
        LABELS.put(SignalType.DIVORCE, "Divorce filing");
        LABELS.put(SignalType.EVICTION, "Eviction filing");
        LABELS.put(SignalType.DEED_TRANSFER, "Recent deed transfer (recently changed hands)");
    }

    /** A signal's contribution halves every 60 days. */
    private static final double HALF_LIFE_DAYS = 60.0;

    /** Tier cutoffs on the final 0–100 score. */
    private static final int HOT_THRESHOLD = 70;
    private static final int WARM_THRESHOLD = 40;

    private final Clock clock;

    public ScoringService(Clock clock) {
        this.clock = clock;
    }

    /**
     * @param signals the resolved homeowner's signals (e.g. {@code ResolvedCluster.signals()})
     * @return the intent score, tier, and strongest-first reasons
     */
    public Score score(List<RawSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            throw new IllegalArgumentException("signals must not be null or empty");
        }

        Instant now = clock.instant();

        // Aggregate each type's recency-decayed contribution so reasons read one line per type.
        Map<SignalType, Double> contributionByType = new EnumMap<>(SignalType.class);
        for (RawSignal signal : signals) {
            double contribution = WEIGHTS.get(signal.signalType()) * decay(signal.observedAt(), now);
            contributionByType.merge(signal.signalType(), contribution, Double::sum);
        }

        double raw = contributionByType.values().stream().mapToDouble(Double::doubleValue).sum();
        int value = (int) Math.round(clamp(raw));

        return new Score(value, tierFor(value), reasons(contributionByType));
    }

    /** {@code 0.5 ^ (ageDays / HALF_LIFE_DAYS)}; future-dated signals are treated as age 0. */
    private static double decay(Instant observedAt, Instant now) {
        double ageDays = Duration.between(observedAt, now).toMillis() / (double) Duration.ofDays(1).toMillis();
        double clampedAge = Math.max(0.0, ageDays);
        return Math.pow(0.5, clampedAge / HALF_LIFE_DAYS);
    }

    private static double clamp(double raw) {
        return Math.max(0.0, Math.min(100.0, raw));
    }

    private static Tier tierFor(int value) {
        if (value >= HOT_THRESHOLD) {
            return Tier.HOT;
        }
        if (value >= WARM_THRESHOLD) {
            return Tier.WARM;
        }
        return Tier.COLD;
    }

    /** One label per signal type present, ordered by absolute contribution (strongest first). */
    private static List<String> reasons(Map<SignalType, Double> contributionByType) {
        List<String> reasons = new ArrayList<>(contributionByType.size());
        contributionByType.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<SignalType, Double> e) -> Math.abs(e.getValue()))
                        .reversed())
                .forEach(e -> reasons.add(LABELS.get(e.getKey())));
        return reasons;
    }
}
