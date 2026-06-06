package com.harbinger.service.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class NameNormalizerTest {

    private final NameNormalizer normalizer = new NameNormalizer();

    @Test
    void fullNameVariantsCollapseToOneKey() {
        // The case/order/punctuation styles the generator applies to a full name
        // must all reduce to the same canonical key.
        String canonical = normalizer.normalize("John Smith");

        assertThat(normalizer.normalize("john smith")).isEqualTo(canonical);
        assertThat(normalizer.normalize("JOHN SMITH")).isEqualTo(canonical);
        assertThat(normalizer.normalize("Smith, John")).isEqualTo(canonical);
        assertThat(canonical).isEqualTo("john smith");
    }

    @Test
    void initialVariantsCollapseToTheirOwnKey() {
        // Initial-only forms cannot reconstruct the first name; they collapse to
        // "j smith" and are reconciled later by Phase 3 fuzzy matching, not here.
        String initialKey = normalizer.normalize("J Smith");

        assertThat(normalizer.normalize("J. Smith")).isEqualTo(initialKey);
        assertThat(initialKey).isEqualTo("j smith");
    }

    @Test
    void reordersLastCommaFirst() {
        assertThat(normalizer.normalize("Smith, John")).isEqualTo("john smith");
    }

    @Test
    void normalizingTwiceEqualsOnce() {
        for (String raw : new String[] {"John Smith", "Smith, John", "J. Smith", "JOHN SMITH"}) {
            String once = normalizer.normalize(raw);
            assertThat(normalizer.normalize(once)).isEqualTo(once);
        }
    }

    @Test
    void collapsesDoubledWhitespace() {
        assertThat(normalizer.normalize("John   Smith")).isEqualTo("john smith");
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> normalizer.normalize(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
