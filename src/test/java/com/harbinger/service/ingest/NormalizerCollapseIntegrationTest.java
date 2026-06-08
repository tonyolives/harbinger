package com.harbinger.service.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import com.harbinger.model.RawSignal;
import com.harbinger.service.SignalGeneratorService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Proves the "collapse to canonical keys" property against real generator output
 * rather than hand-written variants.
 */
class NormalizerCollapseIntegrationTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    private final SignalGeneratorService generator = new SignalGeneratorService(FIXED);
    private final NameNormalizer nameNormalizer = new NameNormalizer();
    private final AddressNormalizer addressNormalizer = new AddressNormalizer();

    @Test
    void oneOwnersMessyVariantsCollapseUnderNormalization() {
        // One owner, every address/name style exercised (6 styles each).
        List<RawSignal> signals = generator.generate(42L, 1, 6);

        Set<String> rawNames =
                signals.stream().map(RawSignal::rawName).collect(Collectors.toSet());
        Set<String> normalizedNames = signals.stream()
                .map(s -> nameNormalizer.normalize(s.rawName()))
                .collect(Collectors.toSet());
        Set<String> normalizedAddresses = signals.stream()
                .map(s -> addressNormalizer.normalize(s.rawAddress()))
                .collect(Collectors.toSet());

        // All address styles are reversible, so they collapse to a single key.
        assertThat(normalizedAddresses).hasSize(1);
        // Names collapse meaningfully; initial-only forms remain a separate key,
        // so we assert reduction rather than full collapse.
        assertThat(normalizedNames.size()).isLessThan(rawNames.size());
    }
}
