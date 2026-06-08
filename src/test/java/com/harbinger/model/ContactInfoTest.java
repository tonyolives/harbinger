package com.harbinger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ContactInfoTest {

    @Test
    void buildsWithValidFields() {
        ContactInfo contact = new ContactInfo("555-0142", "john.smith@example.invalid", true);

        assertThat(contact.phone()).isEqualTo("555-0142");
        assertThat(contact.email()).isEqualTo("john.smith@example.invalid");
        assertThat(contact.mock()).isTrue();
    }

    @Test
    void carriesMockFlagAsGiven() {
        assertThat(new ContactInfo("555-0100", "a@example.invalid", false).mock()).isFalse();
    }

    @Test
    void rejectsNullPhone() {
        assertThatThrownBy(() -> new ContactInfo(null, "a@example.invalid", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void rejectsBlankPhone() {
        assertThatThrownBy(() -> new ContactInfo("  ", "a@example.invalid", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void rejectsNullEmail() {
        assertThatThrownBy(() -> new ContactInfo("555-0142", null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void rejectsBlankEmail() {
        assertThatThrownBy(() -> new ContactInfo("555-0142", "  ", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }
}
