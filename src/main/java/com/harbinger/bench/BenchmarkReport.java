package com.harbinger.bench;

import java.time.Instant;

/**
 * The numbers a fixed-seed benchmark run produces — serialized to {@code benchmarks/report.json}
 * and the source of every metric the README cites (never hand-written). Resolution quality and
 * tier counts are deterministic for a given seed; the signal-to-lead latencies are real
 * wall-clock measurements, so they vary slightly run to run.
 */
public record BenchmarkReport(
        long seed,
        int owners,
        int signalsPerOwner,
        int signalCount,
        int resolvedHomeowners,
        double precision,
        double recall,
        double f1,
        long hotCount,
        long warmCount,
        long coldCount,
        int leadsSurfaced,
        long signalToLeadP50Ms,
        long signalToLeadP95Ms,
        double throughputSignalsPerSec,
        Instant generatedAt) {
}
