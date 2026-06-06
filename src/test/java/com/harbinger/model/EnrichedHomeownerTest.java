package com.harbinger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnrichedHomeownerTest {

    private static final Homeowner OWNER =
            new Homeowner(UUID.randomUUID(), "john smith", "123 main st");
    private static final PropertyDetails PROPERTY = new PropertyDetails(
            PropertyType.SINGLE_FAMILY, 1998, 3, 2, 6000, 400_000L, 500_000L, 200_000L, 300_000L);
    private static final ContactInfo CONTACT =
            new ContactInfo("555-0142", "john.smith@example.invalid", true);

    @Test
    void buildsWithValidFields() {
        EnrichedHomeowner enriched = new EnrichedHomeowner(OWNER, PROPERTY, CONTACT);

        assertThat(enriched.homeowner()).isEqualTo(OWNER);
        assertThat(enriched.property()).isEqualTo(PROPERTY);
        assertThat(enriched.contact()).isEqualTo(CONTACT);
    }

    @Test
    void rejectsNullHomeowner() {
        assertThatThrownBy(() -> new EnrichedHomeowner(null, PROPERTY, CONTACT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("homeowner");
    }

    @Test
    void rejectsNullProperty() {
        assertThatThrownBy(() -> new EnrichedHomeowner(OWNER, null, CONTACT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("property");
    }

    @Test
    void rejectsNullContact() {
        assertThatThrownBy(() -> new EnrichedHomeowner(OWNER, PROPERTY, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contact");
    }
}
