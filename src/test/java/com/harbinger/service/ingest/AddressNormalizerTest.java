package com.harbinger.service.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AddressNormalizerTest {

    private final AddressNormalizer normalizer = new AddressNormalizer();

    @Test
    void allGeneratorAddressStylesCollapseToOneKey() {
        // Mirrors SignalGeneratorService.perturbAddress: clean, suffix-abbreviated,
        // lowercase, uppercase, apartment, and doubled-space variants.
        String canonical = "123 main st";

        assertThat(normalizer.normalize("123 Main Street")).isEqualTo(canonical);
        assertThat(normalizer.normalize("123 Main St")).isEqualTo(canonical);
        assertThat(normalizer.normalize("123 main street")).isEqualTo(canonical);
        assertThat(normalizer.normalize("123 MAIN STREET")).isEqualTo(canonical);
        assertThat(normalizer.normalize("123 Main Street Apt 47")).isEqualTo(canonical);
        assertThat(normalizer.normalize("123  Main Street")).isEqualTo(canonical);
    }

    @Test
    void canonicalizesEveryAbbreviatedSuffix() {
        assertThat(normalizer.normalize("1 Oak Avenue")).isEqualTo("1 oak ave");
        assertThat(normalizer.normalize("1 Oak Boulevard")).isEqualTo("1 oak blvd");
        assertThat(normalizer.normalize("1 Oak Road")).isEqualTo("1 oak rd");
        assertThat(normalizer.normalize("1 Oak Drive")).isEqualTo("1 oak dr");
        assertThat(normalizer.normalize("1 Oak Lane")).isEqualTo("1 oak ln");
    }

    @Test
    void stripsUnitDesignators() {
        assertThat(normalizer.normalize("5 Elm Dr Apt 12")).isEqualTo("5 elm dr");
        assertThat(normalizer.normalize("5 Elm Dr Unit 12")).isEqualTo("5 elm dr");
        assertThat(normalizer.normalize("5 Elm Dr Ste 12")).isEqualTo("5 elm dr");
        assertThat(normalizer.normalize("5 Elm Dr #12")).isEqualTo("5 elm dr");
    }

    @Test
    void normalizingTwiceEqualsOnce() {
        for (String raw : new String[] {"123 Main Street", "123 MAIN STREET", "123 Main St Apt 47"}) {
            String once = normalizer.normalize(raw);
            assertThat(normalizer.normalize(once)).isEqualTo(once);
        }
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
