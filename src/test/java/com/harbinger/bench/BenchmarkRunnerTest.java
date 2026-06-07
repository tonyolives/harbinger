package com.harbinger.bench;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pins the deterministic parts of the benchmark for the demo seed (2, 8 owners × 6 signals):
 * resolution is perfect on the clean set, the tier split is stable, and one lead surfaces per
 * HOT homeowner. Latency is a real measurement, so it's only checked for sanity (non-negative).
 */
class BenchmarkRunnerTest {

    private final BenchmarkReport report = BenchmarkRunner.run(2L, 8, 6);

    @Test
    void resolutionIsPerfectOnTheCleanSet() {
        assertThat(report.signalCount()).isEqualTo(48);
        assertThat(report.resolvedHomeowners()).isEqualTo(8);
        assertThat(report.precision()).isEqualTo(1.0);
        assertThat(report.recall()).isEqualTo(1.0);
        assertThat(report.f1()).isEqualTo(1.0);
    }

    @Test
    void tierSplitIsDeterministic() {
        assertThat(report.hotCount()).isEqualTo(6);
        assertThat(report.warmCount()).isEqualTo(2);
        assertThat(report.coldCount()).isEqualTo(0);
        assertThat(report.hotCount() + report.warmCount() + report.coldCount()).isEqualTo(8);
    }

    @Test
    void oneLeadRowPerResolvedHomeowner() {
        // Every homeowner becomes a live lead row (all tiers), so leads == resolved homeowners.
        assertThat(report.leadsSurfaced()).isEqualTo(report.resolvedHomeowners());
        assertThat(report.leadsSurfaced()).isEqualTo(8);
    }

    @Test
    void latencyAndThroughputAreSane() {
        assertThat(report.signalToLeadP50Ms()).isGreaterThanOrEqualTo(0);
        assertThat(report.signalToLeadP95Ms()).isGreaterThanOrEqualTo(report.signalToLeadP50Ms());
        assertThat(report.throughputSignalsPerSec()).isGreaterThan(0.0);
    }
}
