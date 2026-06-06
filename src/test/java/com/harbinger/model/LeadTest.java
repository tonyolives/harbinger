package com.harbinger.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeadTest {

    private static final Instant SURFACED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Homeowner OWNER =
            new Homeowner(UUID.randomUUID(), "John Smith", "123 Main Street");

    private static Lead lead(int score, Tier tier) {
        return new Lead(OWNER, score, tier, List.of("pre-foreclosure filed"), 42L, SURFACED);
    }

    @Test
    void buildsWithValidFields() {
        Lead lead = lead(80, Tier.HOT);

        assertThat(lead.homeowner()).isEqualTo(OWNER);
        assertThat(lead.intentScore()).isEqualTo(80);
        assertThat(lead.tier()).isEqualTo(Tier.HOT);
        assertThat(lead.reasons()).containsExactly("pre-foreclosure filed");
        assertThat(lead.signalToLeadMs()).isEqualTo(42L);
        assertThat(lead.surfacedAt()).isEqualTo(SURFACED);
    }

    @Test
    void acceptsEveryTier() {
        assertThat(lead(10, Tier.COLD).tier()).isEqualTo(Tier.COLD);
        assertThat(lead(50, Tier.WARM).tier()).isEqualTo(Tier.WARM);
        assertThat(lead(90, Tier.HOT).tier()).isEqualTo(Tier.HOT);
    }

    @Test
    void reasonsAreCopiedAndUnmodifiable() {
        List<String> mutable = new ArrayList<>();
        mutable.add("probate filed");
        Lead lead = new Lead(OWNER, 70, Tier.WARM, mutable, 5L, SURFACED);

        // Mutating the source list must not affect the stored reasons.
        mutable.add("tax delinquency");
        assertThat(lead.reasons()).containsExactly("probate filed");

        assertThatThrownBy(() -> lead.reasons().add("eviction"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNullHomeowner() {
        assertThatThrownBy(() -> new Lead(null, 50, Tier.WARM, List.of(), 1L, SURFACED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("homeowner");
    }

    @Test
    void rejectsScoreBelowZero() {
        assertThatThrownBy(() -> new Lead(OWNER, -1, Tier.WARM, List.of(), 1L, SURFACED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intentScore");
    }

    @Test
    void rejectsScoreAboveHundred() {
        assertThatThrownBy(() -> new Lead(OWNER, 101, Tier.WARM, List.of(), 1L, SURFACED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("intentScore");
    }

    @Test
    void rejectsNullTier() {
        assertThatThrownBy(() -> new Lead(OWNER, 50, null, List.of(), 1L, SURFACED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tier");
    }

    @Test
    void rejectsNullReasons() {
        assertThatThrownBy(() -> new Lead(OWNER, 50, Tier.WARM, null, 1L, SURFACED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reasons");
    }

    @Test
    void rejectsNegativeLatency() {
        assertThatThrownBy(() -> new Lead(OWNER, 50, Tier.WARM, List.of(), -1L, SURFACED))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("signalToLeadMs");
    }

    @Test
    void rejectsNullSurfacedAt() {
        assertThatThrownBy(() -> new Lead(OWNER, 50, Tier.WARM, List.of(), 1L, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("surfacedAt");
    }
}
