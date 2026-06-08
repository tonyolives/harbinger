package com.harbinger.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.RawSignal;
import com.harbinger.service.ingest.NameNormalizer;
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
    void easyModeGivesEachOwnerOneFirstName() {
        // Baseline for the hard-mode contrast: without nicknames, one owner has a single
        // full first name across all spellings (only initial-vs-full variation).
        Set<String> firstNames = fullFirstNames(generator.generate(SEED, 1, 8, false));

        assertThat(firstNames).hasSize(1);
    }

    @Test
    void hardModeSplitsAnOwnerAcrossNicknameForms() {
        // The point of hard mode: one true owner shows up under a formal name AND a
        // nickname (e.g. "robert" and "bob"), which the resolver can't reunite.
        Set<String> firstNames = fullFirstNames(generator.generate(SEED, 1, 8, true));

        assertThat(firstNames).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void hardModeIsDeterministic() {
        assertThat(generator.generate(SEED, 5, 6, true))
                .isEqualTo(generator.generate(SEED, 5, 6, true));
    }

    /** Distinct full (non-initial) first names across the signals, after normalization. */
    private Set<String> fullFirstNames(List<RawSignal> signals) {
        NameNormalizer normalizer = new NameNormalizer();
        return signals.stream()
                .map(s -> normalizer.normalize(s.rawName()).split(" ")[0])
                .filter(token -> token.length() > 1) // drop initial-only forms like "r"
                .collect(Collectors.toSet());
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
