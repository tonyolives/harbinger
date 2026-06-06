package com.harbinger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class HomeownerTest {

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void buildsWithValidFields() {
        Homeowner owner = new Homeowner(ID, "John Smith", "123 Main Street");

        assertThat(owner.id()).isEqualTo(ID);
        assertThat(owner.name()).isEqualTo("John Smith");
        assertThat(owner.address()).isEqualTo("123 Main Street");
    }

    @Test
    void rejectsNullId() {
        assertThatThrownBy(() -> new Homeowner(null, "John Smith", "123 Main Street"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id");
    }

    @Test
    void rejectsNullName() {
        assertThatThrownBy(() -> new Homeowner(ID, null, "123 Main Street"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> new Homeowner(ID, "  ", "123 Main Street"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @Test
    void rejectsNullAddress() {
        assertThatThrownBy(() -> new Homeowner(ID, "John Smith", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("address");
    }

    @Test
    void rejectsBlankAddress() {
        assertThatThrownBy(() -> new Homeowner(ID, "John Smith", "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("address");
    }
}
