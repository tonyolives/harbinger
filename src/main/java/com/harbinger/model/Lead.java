package com.harbinger.model;

import java.time.Instant;
import java.util.List;

/**
 * A ranked, surfaced lead: a {@link Homeowner} the pipeline has scored and
 * decided is worth contacting. Modeled in Phase 1; populated by the scoring and
 * real-time phases later. {@code signalToLeadMs} captures the north-star metric
 * — how long from a signal arriving to this lead surfacing. {@code explanation}
 * is the Phase 6 "why this lead" line built from {@code reasons}.
 */
public record Lead(
        Homeowner homeowner,
        int intentScore,
        Tier tier,
        List<String> reasons,
        String explanation,
        long signalToLeadMs, // Keep an eye on this.
        Instant surfacedAt
) {
    public Lead {
        if (homeowner == null) {
            throw new IllegalArgumentException("homeowner must not be null");
        }
        if (intentScore < 0 || intentScore > 100) {
            throw new IllegalArgumentException("intentScore must be between 0 and 100");
        }
        if (tier == null) {
            throw new IllegalArgumentException("tier must not be null");
        }
        if (reasons == null) {
            throw new IllegalArgumentException("reasons must not be null");
        }
        if (explanation == null || explanation.isBlank()) {
            throw new IllegalArgumentException("explanation must not be blank");
        }
        if (signalToLeadMs < 0) {
            throw new IllegalArgumentException("signalToLeadMs must not be negative");
        }
        if (surfacedAt == null) {
            throw new IllegalArgumentException("surfacedAt must not be null");
        }
        // Defensive copy keeps the record immutable even if the caller mutates its list.
        reasons = List.copyOf(reasons);
    }
}
