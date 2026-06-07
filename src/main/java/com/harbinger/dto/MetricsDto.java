package com.harbinger.dto;

import com.harbinger.model.Tier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Snapshot of the pipeline's live numbers for {@code GET /metrics}: how many signals have been
 * processed, how many leads surfaced, the current tier breakdown across all homeowners, and the
 * signal-to-lead latency percentiles (the north-star metric) over surfaced leads.
 */
public record MetricsDto(
        int signalsProcessed,
        int leadsSurfaced,
        long hotCount,
        long warmCount,
        long coldCount,
        long signalToLeadP50Ms,
        long signalToLeadP95Ms) {

    /**
     * Assemble a snapshot. {@code latenciesMs} is each surfaced lead's {@code signalToLeadMs}.
     */
    public static MetricsDto of(
            int signalsProcessed,
            int leadsSurfaced,
            Map<Tier, Long> tierCounts,
            List<Long> latenciesMs) {
        return new MetricsDto(
                signalsProcessed,
                leadsSurfaced,
                tierCounts.getOrDefault(Tier.HOT, 0L),
                tierCounts.getOrDefault(Tier.WARM, 0L),
                tierCounts.getOrDefault(Tier.COLD, 0L),
                percentile(latenciesMs, 50),
                percentile(latenciesMs, 95));
    }

    /** Nearest-rank percentile; 0 for an empty sample. Shared with the benchmark. */
    public static long percentile(List<Long> values, int p) {
        if (values == null || values.isEmpty()) {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(null);
        int rank = (int) Math.ceil(p / 100.0 * sorted.size());
        int index = Math.min(Math.max(rank, 1), sorted.size()) - 1;
        return sorted.get(index);
    }
}
