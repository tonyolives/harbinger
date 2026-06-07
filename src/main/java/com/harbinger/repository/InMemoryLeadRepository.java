package com.harbinger.repository;

import com.harbinger.model.Lead;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/**
 * In-memory {@link LeadRepository} backed by a {@link ConcurrentHashMap} keyed by homeowner id
 * — thread-safe storage for the in-process pipeline, no database (per {@code TECH_STACK.md}).
 * Saving the same homeowner again replaces its lead, so each homeowner has exactly one live
 * lead.
 */
@Repository
public class InMemoryLeadRepository implements LeadRepository {

    /** Strongest first: highest score, then most-recently surfaced, then id for a stable order. */
    private static final Comparator<Lead> RANKING = Comparator
            .comparingInt(Lead::intentScore).reversed()
            .thenComparing(Comparator.comparing(Lead::surfacedAt).reversed())
            .thenComparing(lead -> lead.homeowner().id());

    private final ConcurrentHashMap<UUID, Lead> leads = new ConcurrentHashMap<>();

    @Override
    public void save(Lead lead) {
        if (lead == null) {
            throw new IllegalArgumentException("lead must not be null");
        }
        leads.put(lead.homeowner().id(), lead);
    }

    @Override
    public Optional<Lead> findById(UUID homeownerId) {
        if (homeownerId == null) {
            throw new IllegalArgumentException("homeownerId must not be null");
        }
        return Optional.ofNullable(leads.get(homeownerId));
    }

    @Override
    public List<Lead> findAllRanked() {
        return leads.values().stream().sorted(RANKING).toList();
    }

    @Override
    public void deleteById(UUID homeownerId) {
        if (homeownerId == null) {
            throw new IllegalArgumentException("homeownerId must not be null");
        }
        leads.remove(homeownerId);
    }

    @Override
    public int count() {
        return leads.size();
    }
}
