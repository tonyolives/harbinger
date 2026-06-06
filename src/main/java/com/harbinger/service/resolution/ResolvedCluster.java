package com.harbinger.service.resolution;

import com.harbinger.model.Homeowner;
import com.harbinger.model.RawSignal;
import java.util.List;
import java.util.UUID;

/**
 * One resolved homeowner identity plus the messy {@link RawSignal}s that collapsed
 * into it. {@link ResolutionService#resolve} produces these; the {@code id} is a
 * deterministic synthetic key (never the ground-truth {@code trueOwnerId}).
 *
 * <p>It carries its member signals so {@link ResolutionService#evaluate} can read
 * each signal's {@code trueOwnerId} when scoring accuracy — the one place that label
 * is allowed.
 */
public record ResolvedCluster(
        UUID id,
        Homeowner homeowner,
        List<RawSignal> signals
) {
    public ResolvedCluster {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (homeowner == null) {
            throw new IllegalArgumentException("homeowner must not be null");
        }
        if (signals == null || signals.isEmpty()) {
            throw new IllegalArgumentException("signals must not be null or empty");
        }
        // Defensive copy keeps the record immutable even if the caller mutates its list.
        signals = List.copyOf(signals);
    }
}
