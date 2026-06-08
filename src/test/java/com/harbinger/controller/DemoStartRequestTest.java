package com.harbinger.controller;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Covers the request normalization: nulls fall back to the canonical demo defaults, and every
 * field is clamped to a safe range so a stray UI input can't spin up a runaway feed.
 */
class DemoStartRequestTest {

    @Test
    void nullsFallBackToDemoDefaults() {
        DemoStartRequest params = new DemoStartRequest(null, null, null, null, null).normalized();

        assertThat(params.seed()).isEqualTo(2L);
        assertThat(params.owners()).isEqualTo(8);
        assertThat(params.signalsPerOwner()).isEqualTo(6);
        assertThat(params.hardMode()).isFalse();
        assertThat(params.feedDelayMs()).isEqualTo(300L);
    }

    @Test
    void valuesWithinRangeArePreserved() {
        DemoStartRequest params = new DemoStartRequest(42L, 12, 4, true, 150L).normalized();

        assertThat(params.seed()).isEqualTo(42L);
        assertThat(params.owners()).isEqualTo(12);
        assertThat(params.signalsPerOwner()).isEqualTo(4);
        assertThat(params.hardMode()).isTrue();
        assertThat(params.feedDelayMs()).isEqualTo(150L);
    }

    @Test
    void outOfRangeValuesAreClamped() {
        DemoStartRequest tooBig = new DemoStartRequest(1L, 999, 999, false, 99999L).normalized();
        assertThat(tooBig.owners()).isEqualTo(50);
        assertThat(tooBig.signalsPerOwner()).isEqualTo(20);
        assertThat(tooBig.feedDelayMs()).isEqualTo(2000L);

        DemoStartRequest tooSmall = new DemoStartRequest(1L, 0, 0, false, -10L).normalized();
        assertThat(tooSmall.owners()).isEqualTo(1);
        assertThat(tooSmall.signalsPerOwner()).isEqualTo(1);
        assertThat(tooSmall.feedDelayMs()).isZero();
    }
}
