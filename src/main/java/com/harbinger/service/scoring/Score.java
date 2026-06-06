package com.harbinger.service.scoring;

import com.harbinger.model.Tier;
import java.util.List;

/**
 * The result of scoring a homeowner's signals: an intent {@code value} in [0, 100], the
 * {@link Tier} it maps to, and human-readable {@code reasons} (strongest first) that later
 * feed {@code Lead.reasons()} and the Phase 6 explanation.
 *
 * <p>Standalone on purpose — it carries only what Phase 5 produces, leaving Phase 7 concerns
 * like {@code signalToLeadMs} and {@code surfacedAt} out of the scoring layer.
 */
public record Score(int value, Tier tier, List<String> reasons) {
    public Score {
        if (value < 0 || value > 100) {
            throw new IllegalArgumentException("value must be between 0 and 100");
        }
        if (tier == null) {
            throw new IllegalArgumentException("tier must not be null");
        }
        if (reasons == null) {
            throw new IllegalArgumentException("reasons must not be null");
        }
        // Defensive copy keeps the record immutable even if the caller mutates its list.
        reasons = List.copyOf(reasons);
    }
}
