package com.harbinger.llm;

import com.harbinger.model.Tier;
import com.harbinger.service.scoring.Score;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * The default {@link LlmProvider}: a deterministic, template-based explanation built straight
 * from a {@link Score}. No API key, no network — this is what the tests and the demo use, and
 * what runs whenever {@code ANTHROPIC_API_KEY} is unset (the opt-in {@code ClaudeLlmProvider}
 * is {@code @Primary} only when the key is present).
 *
 * <p>The output is at most two sentences and adapts to the tier:
 *
 * <pre>
 *   "HOT lead (score 78): Pre-foreclosure filing signals strong intent to sell.
 *    Also flagged: Probate (inherited property), Tax delinquency."
 * </pre>
 *
 * <p>Sentence one frames urgency by {@link Tier} and leads with the strongest reason; sentence
 * two lists the remaining reasons, and is omitted when there is only one. Because it is a pure
 * function of the {@code Score} — no clock, no randomness — the same input always yields the
 * same string, which the deterministic test relies on.
 */
@Service
public class MockLlmProvider implements LlmProvider {

    /** Tier → the urgency clause that closes the first sentence. */
    private static final Map<Tier, String> TIER_PHRASES = new EnumMap<>(Tier.class);

    static {
        TIER_PHRASES.put(Tier.HOT, "strong intent to sell");
        TIER_PHRASES.put(Tier.WARM, "a possible seller worth watching");
        TIER_PHRASES.put(Tier.COLD, "low intent for now");
    }

    @Override
    public String explain(Score score) {
        if (score == null) {
            throw new IllegalArgumentException("score must not be null");
        }
        List<String> reasons = score.reasons();
        if (reasons.isEmpty()) {
            throw new IllegalArgumentException("score must carry at least one reason");
        }

        String topReason = reasons.get(0);
        StringBuilder explanation = new StringBuilder()
                .append(score.tier())
                .append(" lead (score ")
                .append(score.value())
                .append("): ")
                .append(topReason)
                .append(" signals ")
                .append(TIER_PHRASES.get(score.tier()))
                .append('.');

        if (reasons.size() > 1) {
            String rest = String.join(", ", reasons.subList(1, reasons.size()));
            explanation.append(" Also flagged: ").append(rest).append('.');
        }

        return explanation.toString();
    }
}
