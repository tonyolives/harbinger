package com.harbinger.bench;

import com.harbinger.dto.MetricsDto;
import com.harbinger.llm.MockLlmProvider;
import com.harbinger.model.Lead;
import com.harbinger.model.RawSignal;
import com.harbinger.model.Tier;
import com.harbinger.repository.InMemoryLeadRepository;
import com.harbinger.repository.LeadRepository;
import com.harbinger.service.SignalGeneratorService;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import com.harbinger.service.pipeline.LeadEventPublisher;
import com.harbinger.service.pipeline.RealtimeLeadService;
import com.harbinger.service.pipeline.SignalPipeline;
import com.harbinger.service.resolution.ResolutionMetrics;
import com.harbinger.service.resolution.ResolutionService;
import com.harbinger.service.resolution.ResolvedCluster;
import com.harbinger.service.scoring.ScoringService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

/**
 * Runs the whole pipeline on a fixed seed and measures it: resolution precision/recall/F1, the
 * tier breakdown, signal-to-lead latency percentiles, and throughput. Wires the real (pure)
 * services directly — no Spring — so the numbers come from the same code the app runs.
 *
 * <p>Two clocks: a fixed one for generation + scoring (so ages, tiers, and F1 are deterministic
 * for a seed) and the system clock for the orchestrator (so {@code signalToLeadMs} is a real
 * wall-clock measurement). This is the covered logic; {@link Benchmark} only adds the file output.
 */
public final class BenchmarkRunner {

    /** Fixed "now" for generation + scoring, so a seed always yields the same tiers and F1. */
    private static final Instant DATA_NOW = Instant.parse("2026-06-06T12:00:00Z");

    private BenchmarkRunner() {
    }

    public static BenchmarkReport run(long seed, int owners, int signalsPerOwner) {
        Clock dataClock = Clock.fixed(DATA_NOW, ZoneOffset.UTC);
        SignalGeneratorService generator = new SignalGeneratorService(dataClock);
        ResolutionService resolution = new ResolutionService(new NameNormalizer(), new AddressNormalizer());
        SignalPipeline pipeline = new SignalPipeline(resolution, new ScoringService(dataClock));
        LeadRepository repository = new InMemoryLeadRepository();
        LeadEventPublisher noop = lead -> { };
        RealtimeLeadService orchestrator = new RealtimeLeadService(
                pipeline, new MockLlmProvider(), repository, noop, Clock.systemUTC());

        List<RawSignal> signals = generator.generate(seed, owners, signalsPerOwner);

        // Resolution quality is measured on the same signal set (evaluate reads trueOwnerId).
        List<ResolvedCluster> clusters = resolution.resolve(signals);
        ResolutionMetrics metrics = resolution.evaluate(clusters);

        // Drive the realtime loop and time it for throughput.
        long start = System.nanoTime();
        for (RawSignal signal : signals) {
            orchestrator.ingest(signal);
        }
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        double throughput = seconds > 0 ? signals.size() / seconds : 0.0;

        Map<Tier, Long> tiers = orchestrator.tierCounts();
        List<Long> latencies = repository.findAllRanked().stream().map(Lead::signalToLeadMs).toList();

        return new BenchmarkReport(
                seed,
                owners,
                signalsPerOwner,
                signals.size(),
                clusters.size(),
                metrics.precision(),
                metrics.recall(),
                metrics.f1(),
                tiers.getOrDefault(Tier.HOT, 0L),
                tiers.getOrDefault(Tier.WARM, 0L),
                tiers.getOrDefault(Tier.COLD, 0L),
                repository.count(),
                MetricsDto.percentile(latencies, 50),
                MetricsDto.percentile(latencies, 95),
                throughput,
                Instant.now());
    }
}
