package com.harbinger.service.pipeline;

import com.harbinger.llm.LlmProvider;
import com.harbinger.model.Lead;
import com.harbinger.model.RawSignal;
import com.harbinger.model.Tier;
import com.harbinger.repository.LeadRepository;
import com.harbinger.service.scoring.Score;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * The real-time loop. Signals arrive one at a time via {@link #ingest}; each arrival re-runs the
 * {@link SignalPipeline} over every signal seen so far and turns each resolved homeowner into a
 * live {@link Lead} row. A homeowner's lead is created the first time it is scored (at whatever
 * tier) and updated in place as more signals arrive; HOT leads rank to the top. The
 * {@code signalToLeadMs} north-star metric is measured from the triggering signal's arrival to
 * the moment the lead first surfaces.
 *
 * <p>Re-running the whole batch each tick keeps the existing pure services (resolution, scoring)
 * reused verbatim; it's O(n²) but trivial at demo scale. An existing lead is only rewritten —
 * and only then re-published and re-explained — when its rounded score or its reasons actually
 * change, so the badge and the explanation always agree and steady-state ticks cost nothing.
 * The explanation is regenerated on every such change (one LLM call per change: free for the
 * default Mock provider, bounded for the opt-in Claude path); latency and surfaced-at are kept
 * from first surfacing.
 *
 * <p>Homeowner identity is the resolution cluster id. A stable cross-tick entity id is a v2
 * concern; at demo scale (unique addresses, representative names that stabilize immediately) the
 * id does not drift. All mutable state is guarded by {@code synchronized} so a background demo
 * driver and HTTP reads can share one instance safely.
 */
@Service
public class RealtimeLeadService {

    private final SignalPipeline pipeline;
    private final LlmProvider llmProvider;
    private final LeadRepository repository;
    private final LeadEventPublisher publisher;
    private final Clock clock;

    private final List<RawSignal> signals = new ArrayList<>();
    private Map<Tier, Long> tierCounts = new EnumMap<>(Tier.class);
    private int signalsProcessed = 0;

    public RealtimeLeadService(
            SignalPipeline pipeline,
            LlmProvider llmProvider,
            LeadRepository repository,
            LeadEventPublisher publisher,
            Clock clock) {
        this.pipeline = pipeline;
        this.llmProvider = llmProvider;
        this.repository = repository;
        this.publisher = publisher;
        this.clock = clock;
    }

    /**
     * Feed one signal through the pipeline.
     *
     * @return the leads created or updated by this signal (empty when nothing changed)
     */
    public synchronized List<Lead> ingest(RawSignal signal) {
        if (signal == null) {
            throw new IllegalArgumentException("signal must not be null");
        }

        Instant arrival = clock.instant();
        signals.add(signal);
        signalsProcessed++;

        List<ScoredHomeowner> scored = pipeline.score(signals);

        // One "now" for everything touched on this tick, so signalToLeadMs is measured once.
        Instant surfaceTime = clock.instant();
        long signalToLeadMs = Duration.between(arrival, surfaceTime).toMillis();

        List<Lead> touched = new ArrayList<>();
        Map<Tier, Long> counts = new EnumMap<>(Tier.class);
        Set<UUID> currentIds = new HashSet<>();
        for (ScoredHomeowner sh : scored) {
            Score score = sh.score();
            counts.merge(score.tier(), 1L, Long::sum);
            currentIds.add(sh.homeowner().id());

            Optional<Lead> existing = repository.findById(sh.homeowner().id());
            if (existing.isEmpty()) {
                touched.add(surface(sh, score, signalToLeadMs, surfaceTime));
            } else if (changed(existing.get(), score)) {
                touched.add(refresh(sh, score, existing.get()));
            }
        }
        pruneDriftedLeads(currentIds);
        tierCounts = counts;
        return touched;
    }

    /**
     * Drop any stored lead whose homeowner id is no longer produced by the current resolution.
     * As more signals arrive, a homeowner's cluster id can change (e.g. "J Pollich" consolidates
     * into "Jean Pollich"); pruning keeps one row per current homeowner instead of leaving the
     * earlier, tentative identity behind. Clients learn the new state by re-reading {@code /leads}
     * after the accompanying {@code lead} event.
     */
    private void pruneDriftedLeads(Set<UUID> currentIds) {
        for (Lead stored : repository.findAllRanked()) {
            if (!currentIds.contains(stored.homeowner().id())) {
                repository.deleteById(stored.homeowner().id());
            }
        }
    }

    /** First time we see a homeowner: create its lead at the current tier, explain, publish. */
    private Lead surface(ScoredHomeowner sh, Score score, long signalToLeadMs, Instant surfaceTime) {
        Lead lead = new Lead(
                sh.homeowner(), score.value(), score.tier(), score.reasons(),
                llmProvider.explain(score), signalToLeadMs, surfaceTime);
        repository.save(lead);
        publisher.publish(lead);
        return lead;
    }

    /** Score or reasons changed: rewrite with a fresh explanation, keep latency + surfaced-at. */
    private Lead refresh(ScoredHomeowner sh, Score score, Lead existing) {
        Lead lead = new Lead(
                sh.homeowner(), score.value(), score.tier(), score.reasons(),
                llmProvider.explain(score), existing.signalToLeadMs(), existing.surfacedAt());
        repository.save(lead);
        publisher.publish(lead);
        return lead;
    }

    /** A stored lead is stale when its rounded score or its reasons no longer match the latest. */
    private static boolean changed(Lead existing, Score score) {
        return existing.intentScore() != score.value() || !existing.reasons().equals(score.reasons());
    }

    /** Total signals fed in so far. */
    public synchronized int signalsProcessed() {
        return signalsProcessed;
    }

    /** Number of homeowners surfaced as leads (one row per homeowner). */
    public synchronized int leadsSurfaced() {
        return repository.count();
    }

    /** Current count of homeowners in each tier (from the latest pass). */
    public synchronized Map<Tier, Long> tierCounts() {
        return new EnumMap<>(tierCounts);
    }
}
