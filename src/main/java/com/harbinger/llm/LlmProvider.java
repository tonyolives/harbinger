package com.harbinger.llm;

import com.harbinger.service.scoring.Score;

/**
 * Phase 6: turns a homeowner's {@link Score} into a short, human-readable "why this lead"
 * explanation. Kept as an interface so the explanation source is decoupled from the pipeline:
 * the default {@link MockLlmProvider} builds a deterministic template (no API key, no network),
 * while the opt-in {@code ClaudeLlmProvider} calls the Claude Messages API when
 * {@code ANTHROPIC_API_KEY} is set.
 */
public interface LlmProvider {

    /**
     * @param score the intent score (value, tier, strongest-first reasons) from Phase 5
     * @return a non-empty, at-most-two-sentence explanation built from the score's reasons
     * @throws IllegalArgumentException if {@code score} is null or carries no reasons
     */
    String explain(Score score);
}
