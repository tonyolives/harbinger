package com.harbinger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.RawSignal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class SignalGeneratorServiceTest {

    private static final long SEED = 42L;
    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    private final SignalGeneratorService generator = new SignalGeneratorService(FIXED);

    @Test
    void sameSeedProducesIdenticalOutput() {
        List<RawSignal> first = generator.generate(SEED, 5, 4);
        List<RawSignal> second = generator.generate(SEED, 5, 4);

        // Records give value-based equals, so the whole streams must match.
        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentSeedProducesDifferentOutput() {
        List<RawSignal> a = generator.generate(SEED, 5, 4);
        List<RawSignal> b = generator.generate(SEED + 1, 5, 4);

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void emitsRequestedNumberOfSignals() {
        assertThat(generator.generate(SEED, 3, 4)).hasSize(12);
    }

    @Test
    void oneOwnerYieldsMessyVariantsSharingOneTrueOwnerId() {
        List<RawSignal> signals = generator.generate(SEED, 1, 5);

        Set<UUID> ownerIds = signals.stream()
                .map(RawSignal::trueOwnerId)
                .collect(Collectors.toSet());
        assertThat(ownerIds).hasSize(1);

        // The point of Phase 1: the same homeowner shows up under several spellings.
        Set<String> names = signals.stream().map(RawSignal::rawName).collect(Collectors.toSet());
        Set<String> addresses = signals.stream().map(RawSignal::rawAddress).collect(Collectors.toSet());
        assertThat(names).hasSizeGreaterThan(1);
        assertThat(addresses).hasSizeGreaterThan(1);
    }

    @Test
    void distinctOwnersGetDistinctTrueOwnerIds() {
        List<RawSignal> signals = generator.generate(SEED, 4, 1);

        Set<UUID> ownerIds = signals.stream()
                .map(RawSignal::trueOwnerId)
                .collect(Collectors.toSet());
        assertThat(ownerIds).hasSize(4);
    }

    @Test
    void everySignalHasRequiredFieldsAndObservedAtNotInFuture() {
        List<RawSignal> signals = generator.generate(SEED, 5, 4);

        assertThat(signals).allSatisfy(signal -> {
            assertThat(signal.rawName()).isNotBlank();
            assertThat(signal.rawAddress()).isNotBlank();
            assertThat(signal.source()).isNotNull();
            assertThat(signal.signalType()).isNotNull();
            assertThat(signal.observedAt()).isBeforeOrEqualTo(FIXED.instant());
            assertThat(signal.trueOwnerId()).isNotNull();
        });
    }

    @Test
    void rejectsNonPositiveOwnerCount() {
        assertThatThrownBy(() -> generator.generate(SEED, 0, 4))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerCount");
    }

    @Test
    void rejectsNonPositiveSignalsPerOwner() {
        assertThatThrownBy(() -> generator.generate(SEED, 4, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signalsPerOwner");
    }
}
