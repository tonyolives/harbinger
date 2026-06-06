package com.harbinger;

import com.harbinger.model.RawSignal;
import com.harbinger.service.SignalGeneratorService;
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

    private static final long DEMO_SEED = 3L;
    private static final int OWNERS = 8;
    private static final int SIGNALS_PER_OWNER = 10;

    private final SignalGeneratorService generator;
    private final ResolutionService resolutionService;

    public DemoRunner(SignalGeneratorService generator, ResolutionService resolutionService) {
        this.generator = generator;
        this.resolutionService = resolutionService;
    }

    @Override
    public void run(String... args) {
        List<RawSignal> signals = generator.generate(DEMO_SEED, OWNERS, SIGNALS_PER_OWNER);
        System.out.printf("Generated %d messy signals (seed=%d)%n", signals.size(), DEMO_SEED);
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
    }
}
