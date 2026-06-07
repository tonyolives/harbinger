package com.harbinger.service.pipeline;

import com.harbinger.model.Lead;

/**
 * Pushes a freshly-surfaced {@link Lead} to live subscribers. The orchestrator depends only on
 * this interface so it stays free of any transport detail; the SSE implementation arrives with
 * the web layer. Until then a no-op fallback bean keeps the context wired (see
 * {@code AppConfig}).
 */
public interface LeadEventPublisher {

    /** Publish a newly surfaced lead. Best-effort: failures to reach a subscriber are swallowed. */
    void publish(Lead lead);
}
