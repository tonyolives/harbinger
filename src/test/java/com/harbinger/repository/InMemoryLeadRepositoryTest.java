package com.harbinger.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.Homeowner;
import com.harbinger.model.Lead;
import com.harbinger.model.Tier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Covers the in-memory store: save/find by homeowner id, one-lead-per-homeowner replacement,
 * and the strongest-first ranking (score desc, then most-recently surfaced).
 */
class InMemoryLeadRepositoryTest {

    private final InMemoryLeadRepository repository = new InMemoryLeadRepository();

    private static Lead lead(String name, int score, Tier tier, Instant surfacedAt) {
        Homeowner owner = new Homeowner(UUID.randomUUID(), name, "1 Main St");
        return new Lead(owner, score, tier, List.of("reason"), "why", 1L, surfacedAt);
    }

    @Test
    void savesAndFindsByHomeownerId() {
        Lead saved = lead("john smith", 80, Tier.HOT, Instant.parse("2026-06-06T00:00:00Z"));
        repository.save(saved);

        assertThat(repository.findById(saved.homeowner().id())).contains(saved);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void findByIdEmptyWhenAbsent() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void savingSameHomeownerReplacesItsLead() {
        Homeowner owner = new Homeowner(UUID.randomUUID(), "john smith", "1 Main St");
        repository.save(new Lead(owner, 70, Tier.WARM, List.of("a"), "why", 1L,
                Instant.parse("2026-06-06T00:00:00Z")));
        repository.save(new Lead(owner, 95, Tier.HOT, List.of("b"), "why2", 1L,
                Instant.parse("2026-06-06T01:00:00Z")));

        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(owner.id()).orElseThrow().intentScore()).isEqualTo(95);
    }

    @Test
    void findAllRankedOrdersByScoreThenRecency() {
        Lead cold = lead("a", 30, Tier.COLD, Instant.parse("2026-06-06T00:00:00Z"));
        Lead hotOld = lead("b", 90, Tier.HOT, Instant.parse("2026-06-06T00:00:00Z"));
        Lead hotNew = lead("c", 90, Tier.HOT, Instant.parse("2026-06-06T02:00:00Z"));
        repository.save(cold);
        repository.save(hotOld);
        repository.save(hotNew);

        // Same score → most-recently surfaced first; lower score last.
        assertThat(repository.findAllRanked()).containsExactly(hotNew, hotOld, cold);
    }

    @Test
    void deleteByIdRemovesTheLead() {
        Lead saved = lead("john smith", 80, Tier.HOT, Instant.parse("2026-06-06T00:00:00Z"));
        repository.save(saved);

        repository.deleteById(saved.homeowner().id());

        assertThat(repository.count()).isZero();
        assertThat(repository.findById(saved.homeowner().id())).isEmpty();
    }

    @Test
    void nullArgumentsRejected() {
        assertThatThrownBy(() -> repository.save(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.findById(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> repository.deleteById(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
