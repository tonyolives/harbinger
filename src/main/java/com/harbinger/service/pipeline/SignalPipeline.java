package com.harbinger.service.pipeline;

import com.harbinger.model.RawSignal;
import com.harbinger.service.resolution.ResolutionService;
import com.harbinger.service.resolution.ResolvedCluster;
import com.harbinger.service.scoring.ScoringService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Composes the resolve → score stages into one pass: given the raw signals seen so far, it
 * resolves them into homeowners and scores each, returning a {@link ScoredHomeowner} per
 * homeowner. It deliberately stops short of the explanation step — building the "why this
 * lead" line is an LLM call, so the orchestrator runs it only for leads that actually surface,
 * not for every homeowner on every pass.
 *
 * <p>Pure given the {@link java.time.Clock} that {@link ScoringService} already injects: the
 * same signals at the same instant always yield the same result.
 */
@Service
public class SignalPipeline {

    private final ResolutionService resolutionService;
    private final ScoringService scoringService;

    public SignalPipeline(ResolutionService resolutionService, ScoringService scoringService) {
        this.resolutionService = resolutionService;
        this.scoringService = scoringService;
    }

    /**
     * @param signals every raw signal seen so far (in any order)
     * @return one scored homeowner per resolved cluster; empty when there are no signals
     */
    public List<ScoredHomeowner> score(List<RawSignal> signals) {
        if (signals == null) {
            throw new IllegalArgumentException("signals must not be null");
        }

        List<ResolvedCluster> clusters = resolutionService.resolve(signals);
        List<ScoredHomeowner> scored = new ArrayList<>(clusters.size());
        for (ResolvedCluster cluster : clusters) {
            scored.add(new ScoredHomeowner(cluster.homeowner(), scoringService.score(cluster.signals())));
        }
        return scored;
    }
}
