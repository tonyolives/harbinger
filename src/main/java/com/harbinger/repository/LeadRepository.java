package com.harbinger.repository;

import com.harbinger.model.Lead;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Stores the leads the pipeline has surfaced, keyed by homeowner id (one live lead per
 * homeowner — re-scoring an already-surfaced homeowner updates its lead in place). In-memory
 * only for the demo (see {@link InMemoryLeadRepository}); the interface keeps the rest of the
 * app from depending on that choice.
 */
public interface LeadRepository {

    /** Insert or replace the lead for its homeowner. */
    void save(Lead lead);

    /** @param homeownerId the resolved homeowner's id */
    Optional<Lead> findById(UUID homeownerId);

    /** All leads, strongest first (score desc, then most-recently surfaced). */
    List<Lead> findAllRanked();

    /** Number of stored leads. */
    int count();
}
