package com.harbinger;

import com.harbinger.model.EnrichedHomeowner;
import com.harbinger.model.RawSignal;
import com.harbinger.service.SignalGeneratorService;
import com.harbinger.service.enrich.EnrichmentService;
import com.harbinger.service.resolution.ResolutionMetrics;
import com.harbinger.service.resolution.ResolvedCluster;
import com.harbinger.service.resolution.ResolutionService;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Phase 1 "done" check: on {@code ./mvnw spring-boot:run}, print a deterministic
 * stream of labeled messy signals. Pure presentation glue — all logic lives in
 * (and is tested via) {@link SignalGeneratorService}; this class is excluded from
 * the coverage gate, like the application entry point.
 */
@Component
public class DemoRunner implements CommandLineRunner {

    private static final long DEMO_SEED = 1L;
    private static final int OWNERS = 8;
    private static final int SIGNALS_PER_OWNER = 6;

    // Flip to true to inject nicknames the resolver can't reunite and watch the printed
    // recall/F1 fall below 1.0 — a live look at where the rules-based v1 breaks.
    private static final boolean HARD_MODE = true;

    private final SignalGeneratorService generator;
    private final ResolutionService resolutionService;
    private final EnrichmentService enrichmentService;

    public DemoRunner(
            SignalGeneratorService generator,
            ResolutionService resolutionService,
            EnrichmentService enrichmentService) {
        this.generator = generator;
        this.resolutionService = resolutionService;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public void run(String... args) {
        List<RawSignal> signals = generator.generate(DEMO_SEED, OWNERS, SIGNALS_PER_OWNER, HARD_MODE);
        System.out.printf(
                "Generated %d messy signals (seed=%d, hardMode=%b)%n",
                signals.size(), DEMO_SEED, HARD_MODE);
        for (RawSignal s : signals) {
            System.out.printf(
                    "[owner=%s src=%-15s type=%-16s at=%s] \"%s\" @ \"%s\"%n",
                    s.trueOwnerId(), s.source(), s.signalType(), s.observedAt(),
                    s.rawName(), s.rawAddress());
        }

        List<ResolvedCluster> clusters = resolutionService.resolve(signals);
        ResolutionMetrics metrics = resolutionService.evaluate(clusters);
        System.out.printf(
                "Resolved %d signals into %d homeowners (true owners=%d) | "
                        + "precision=%.3f recall=%.3f f1=%.3f%n",
                signals.size(), clusters.size(), OWNERS,
                metrics.precision(), metrics.recall(), metrics.f1());

        // Enrich each resolved homeowner with mock property + contact details; print one
        // as a sample so the Phase 4 step is visible end to end.
        for (ResolvedCluster cluster : clusters) {
            EnrichedHomeowner enriched = enrichmentService.enrich(cluster.homeowner());
            System.out.printf(
                    "Enriched \"%s\" @ \"%s\" | %s built %d, %dbd/%dba, equity $%,d | "
                            + "contact %s / %s (mock=%b)%n",
                    enriched.homeowner().name(), enriched.homeowner().address(),
                    enriched.property().propertyType(), enriched.property().yearBuilt(),
                    enriched.property().beds(), enriched.property().baths(),
                    enriched.property().equity(),
                    enriched.contact().phone(), enriched.contact().email(),
                    enriched.contact().mock());
        }
    }
}
