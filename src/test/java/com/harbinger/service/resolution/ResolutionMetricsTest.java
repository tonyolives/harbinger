package com.harbinger.service.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ResolutionMetricsTest {

    @Test
    void ofComputesF1AsHarmonicMean() {
        // TP=1, predicted=2, true=4 -> precision=0.5, recall=0.25, f1=2*.5*.25/.75.
        ResolutionMetrics m = ResolutionMetrics.of(1, 2, 4);

        assertThat(m.precision()).isEqualTo(0.5);
        assertThat(m.recall()).isEqualTo(0.25);
        assertThat(m.f1()).isEqualTo(2 * 0.5 * 0.25 / 0.75);
    }

    @Test
    void zeroDenominatorsYieldPerfectRatios() {
        ResolutionMetrics m = ResolutionMetrics.of(0, 0, 0);

        assertThat(m.precision()).isEqualTo(1.0);
        assertThat(m.recall()).isEqualTo(1.0);
        assertThat(m.f1()).isEqualTo(1.0);
    }

    @Test
    void rejectsRatiosOutOfRange() {
        assertThatThrownBy(() -> new ResolutionMetrics(1.5, 1.0, 1.0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolutionMetrics(1.0, -0.1, 1.0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolutionMetrics(1.0, 1.0, 2.0, 0, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeCounts() {
        assertThatThrownBy(() -> new ResolutionMetrics(1.0, 1.0, 1.0, -1, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolutionMetrics(1.0, 1.0, 1.0, 0, -1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolutionMetrics(1.0, 1.0, 1.0, 0, 0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
