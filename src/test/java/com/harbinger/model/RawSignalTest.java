package com.harbinger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RawSignalTest {

    private static final Instant OBSERVED = Instant.parse("2026-01-01T00:00:00Z");
    private static final UUID OWNER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void buildsWithValidFields() {
        RawSignal signal = new RawSignal(
                "J Smith", "123 Main St", Source.COURT, SignalType.PROBATE, OBSERVED, OWNER);

        assertThat(signal.rawName()).isEqualTo("J Smith");
        assertThat(signal.rawAddress()).isEqualTo("123 Main St");
        assertThat(signal.source()).isEqualTo(Source.COURT);
        assertThat(signal.signalType()).isEqualTo(SignalType.PROBATE);
        assertThat(signal.observedAt()).isEqualTo(OBSERVED);
        assertThat(signal.trueOwnerId()).isEqualTo(OWNER);
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new RawSignal(
                null, "123 Main St", Source.COURT, SignalType.PROBATE, OBSERVED, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawName");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new RawSignal(
                " ", "123 Main St", Source.COURT, SignalType.PROBATE, OBSERVED, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawName");
    }

    @Test
    void rejectsNullAddress() {
        assertThatThrownBy(() -> new RawSignal(
                "J Smith", null, Source.COURT, SignalType.PROBATE, OBSERVED, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawAddress");
    }

    @Test
    void rejectsBlankAddress() {
        assertThatThrownBy(() -> new RawSignal(
                "J Smith", "", Source.COURT, SignalType.PROBATE, OBSERVED, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("rawAddress");
    }

    @Test
    void rejectsNullSource() {
        assertThatThrownBy(() -> new RawSignal(
                "J Smith", "123 Main St", null, SignalType.PROBATE, OBSERVED, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("source");
    }

    @Test
    void rejectsNullSignalType() {
        assertThatThrownBy(() -> new RawSignal(
                "J Smith", "123 Main St", Source.COURT, null, OBSERVED, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signalType");
    }

    @Test
    void rejectsNullObservedAt() {
        assertThatThrownBy(() -> new RawSignal(
                "J Smith", "123 Main St", Source.COURT, SignalType.PROBATE, null, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("observedAt");
    }

    @Test
    void rejectsNullTrueOwnerId() {
        assertThatThrownBy(() -> new RawSignal(
                "J Smith", "123 Main St", Source.COURT, SignalType.PROBATE, OBSERVED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trueOwnerId");
    }
}
