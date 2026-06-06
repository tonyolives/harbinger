package com.harbinger.service.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.Homeowner;
import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ResolvedClusterTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final Homeowner OWNER = new Homeowner(ID, "john smith", "123 main st");
    private static final RawSignal SIGNAL = new RawSignal(
            "John Smith", "123 Main St", Source.COURT, SignalType.PROBATE,
            Instant.parse("2026-06-01T12:00:00Z"), UUID.randomUUID());

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> new ResolvedCluster(null, OWNER, List.of(SIGNAL)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullHomeowner() {
        assertThatThrownBy(() -> new ResolvedCluster(ID, null, List.of(SIGNAL)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNullOrEmptySignals() {
        assertThatThrownBy(() -> new ResolvedCluster(ID, OWNER, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ResolvedCluster(ID, OWNER, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void copiesSignalsDefensively() {
        List<RawSignal> mutable = new ArrayList<>(List.of(SIGNAL));
        ResolvedCluster cluster = new ResolvedCluster(ID, OWNER, mutable);
        mutable.clear();
        assertThat(cluster.signals()).hasSize(1);
    }
}
