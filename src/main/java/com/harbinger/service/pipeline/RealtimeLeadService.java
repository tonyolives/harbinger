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
 * {@link SignalPipeline} over every signal seen so far and watches for a homeowner crossing into
 * the HOT tier. The first time a homeowner is HOT, exactly one {@link Lead} is surfaced —
 * explained (one LLM call), stored, and published — and the {@code signalToLeadMs} north-star
 * metric is measured from the signal's arrival to that moment.
 *
 * <p>Re-running the whole batch each tick keeps the existing pure services (resolution, scoring)
 * reused verbatim; it's O(n²) but trivial at demo scale. Already-surfaced homeowners are refreshed
 * in place (score/tier/reasons) so {@code /leads} stays current, but they never re-alert and never
 * trigger a second LLM call — the explanation, latency, and surfaced-at time are kept from the
 * first crossing.
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
    private final Set<UUID> surfaced = new HashSet<>();
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
     * @return the lead surfaced by this signal (a homeowner crossing into HOT), or empty if none
     */
    public synchronized Optional<Lead> ingest(RawSignal signal) {
        if (signal == null) {
            throw new IllegalArgumentException("signal must not be null");
        }

        Instant arrival = clock.instant();
        signals.add(signal);
        signalsProcessed++;

        List<ScoredHomeowner> scored = pipeline.score(signals);

        // One "now" for everything surfaced on this tick, so signalToLeadMs is measured once.
        Instant surfaceTime = clock.instant();
        long signalToLeadMs = Duration.between(arrival, surfaceTime).toMillis();

        Optional<Lead> surfacedLead = Optional.empty();
        Map<Tier, Long> counts = new EnumMap<>(Tier.class);
        for (ScoredHomeowner sh : scored) {
            UUID id = sh.homeowner().id();
            Score score = sh.score();
            counts.merge(score.tier(), 1L, Long::sum);

            if (!surfaced.contains(id)) {
                if (score.tier() == Tier.HOT) {
                    surfaced.add(id);
                    String explanation = llmProvider.explain(score);
                    Lead lead = new Lead(
                            sh.homeowner(), score.value(), score.tier(), score.reasons(),
                            explanation, signalToLeadMs, surfaceTime);
                    repository.save(lead);
                    publisher.publish(lead);
                    surfacedLead = Optional.of(lead);
                }
            } else {
                refreshSurfacedLead(sh, score);
            }
        }
        tierCounts = counts;
        return surfacedLead;
    }

    /** Update an already-surfaced lead's score/tier/reasons; keep its explanation, latency, time. */
    private void refreshSurfacedLead(ScoredHomeowner sh, Score score) {
        repository.findById(sh.homeowner().id()).ifPresent(existing -> repository.save(new Lead(
                sh.homeowner(), score.value(), score.tier(), score.reasons(),
                existing.explanation(), existing.signalToLeadMs(), existing.surfacedAt())));
    }

    /** Total signals fed in so far. */
    public synchronized int signalsProcessed() {
        return signalsProcessed;
    }

    /** Number of homeowners that have been surfaced as leads. */
    public synchronized int leadsSurfaced() {
        return surfaced.size();
    }

    /** Current count of homeowners in each tier (from the latest pass). */
    public synchronized Map<Tier, Long> tierCounts() {
        return new EnumMap<>(tierCounts);
    }
}
