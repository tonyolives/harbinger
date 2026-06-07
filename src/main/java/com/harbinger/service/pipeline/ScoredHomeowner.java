package com.harbinger.service.pipeline;

import com.harbinger.model.Homeowner;
import com.harbinger.service.scoring.Score;

/**
 * A resolved {@link Homeowner} paired with its current {@link Score} — the output of one
 * {@link SignalPipeline} pass. The realtime orchestrator compares these across passes to spot
 * tier changes and decide when to surface a lead.
 */
public record ScoredHomeowner(Homeowner homeowner, Score score) {
    public ScoredHomeowner {
        if (homeowner == null) {
            throw new IllegalArgumentException("homeowner must not be null");
        }
        if (score == null) {
            throw new IllegalArgumentException("score must not be null");
        }
    }
}
