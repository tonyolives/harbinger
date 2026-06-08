package com.harbinger.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.harbinger.model.Tier;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MetricsDtoTest {

    @Test
    void ofMapsTierCountsAndPercentiles() {
        MetricsDto metrics = MetricsDto.of(
                12, 3,
                Map.of(Tier.HOT, 3L, Tier.WARM, 1L, Tier.COLD, 5L),
                List.of(10L, 20L, 30L, 40L, 50L));

        assertThat(metrics.signalsProcessed()).isEqualTo(12);
        assertThat(metrics.leadsSurfaced()).isEqualTo(3);
        assertThat(metrics.hotCount()).isEqualTo(3);
        assertThat(metrics.warmCount()).isEqualTo(1);
        assertThat(metrics.coldCount()).isEqualTo(5);
        assertThat(metrics.signalToLeadP50Ms()).isEqualTo(30); // nearest-rank of 5 values
        assertThat(metrics.signalToLeadP95Ms()).isEqualTo(50);
    }

    @Test
    void missingTiersAndEmptyLatenciesDefaultToZero() {
        MetricsDto metrics = MetricsDto.of(0, 0, Map.of(), List.of());

        assertThat(metrics.hotCount()).isZero();
        assertThat(metrics.warmCount()).isZero();
        assertThat(metrics.coldCount()).isZero();
        assertThat(metrics.signalToLeadP50Ms()).isZero();
        assertThat(metrics.signalToLeadP95Ms()).isZero();
    }

    @Test
    void percentileIsNearestRank() {
        assertThat(MetricsDto.percentile(List.of(5L), 50)).isEqualTo(5);
        assertThat(MetricsDto.percentile(List.of(1L, 2L, 3L, 4L), 50)).isEqualTo(2);
        assertThat(MetricsDto.percentile(List.of(), 90)).isZero();
    }
}
