package com.harbinger.service.resolution;

import static org.assertj.core.api.Assertions.assertThat;

import com.harbinger.model.Homeowner;
import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import com.harbinger.service.SignalGeneratorService;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * End-to-end accuracy of resolution against real generator output. This is the proof the
 * pipeline works: it wires the real normalizers, generator, and {@link ResolutionService}
 * and scores them with {@link ResolutionService#evaluate}.
 */
class ResolutionAccuracyIntegrationTest {

    private static final long SEED = 7L;
    private static final int OWNERS = 15;
    private static final int SIGNALS_PER_OWNER = 6;
    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    private final NameNormalizer nameNormalizer = new NameNormalizer();
    private final AddressNormalizer addressNormalizer = new AddressNormalizer();
    private final ResolutionService service =
            new ResolutionService(nameNormalizer, addressNormalizer);
    private final SignalGeneratorService generator = new SignalGeneratorService(FIXED);

    @Test
    void cleanSetResolvesPerfectly() {
        // No noise: each owner's signals share one exact spelling, so resolution is trivial.
        List<RawSignal> clean = new ArrayList<>();
        for (int owner = 0; owner < 5; owner++) {
            UUID trueOwnerId = UUID.fromString("00000000-0000-0000-0000-00000000000" + owner);
            for (int i = 0; i < 4; i++) {
                clean.add(new RawSignal(
                        "Owner " + owner, owner + " Clean St",
                        Source.COURT, SignalType.PROBATE, FIXED.instant(), trueOwnerId));
            }
        }

        ResolutionMetrics m = service.evaluate(service.resolve(clean));

        assertThat(m.precision()).isEqualTo(1.0);
        assertThat(m.recall()).isEqualTo(1.0);
    }

    @Test
    void noisySetMeetsAccuracyTargets() {
        List<RawSignal> signals = generator.generate(SEED, OWNERS, SIGNALS_PER_OWNER);

        ResolutionMetrics m = service.evaluate(service.resolve(signals));

        assertThat(m.precision()).isGreaterThanOrEqualTo(0.95);
        assertThat(m.recall()).isGreaterThanOrEqualTo(0.85);
    }

    @Test
    void fuzzyResolutionBeatsNaiveExactMatch() {
        List<RawSignal> signals = generator.generate(SEED, OWNERS, SIGNALS_PER_OWNER);

        ResolutionMetrics fuzzy = service.evaluate(service.resolve(signals));
        ResolutionMetrics naive = service.evaluate(naiveExactMatchClusters(signals));

        // The naive baseline can't bridge "j smith" / "john smith", so it under-merges.
        assertThat(fuzzy.recall()).isGreaterThan(naive.recall());
        assertThat(fuzzy.f1()).isGreaterThanOrEqualTo(naive.f1());
    }

    /** Baseline: group signals only when their normalized name AND address match exactly. */
    private List<ResolvedCluster> naiveExactMatchClusters(List<RawSignal> signals) {
        Map<String, List<RawSignal>> groups = new LinkedHashMap<>();
        Map<String, String[]> keyParts = new LinkedHashMap<>();
        for (RawSignal signal : signals) {
            String name = nameNormalizer.normalize(signal.rawName());
            String address = addressNormalizer.normalize(signal.rawAddress());
            String key = name + "|" + address;
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(signal);
            keyParts.putIfAbsent(key, new String[] {name, address});
        }

        List<ResolvedCluster> clusters = new ArrayList<>(groups.size());
        for (Map.Entry<String, List<RawSignal>> entry : groups.entrySet()) {
            String[] parts = keyParts.get(entry.getKey());
            UUID id = UUID.nameUUIDFromBytes(entry.getKey().getBytes(StandardCharsets.UTF_8));
            clusters.add(new ResolvedCluster(id, new Homeowner(id, parts[0], parts[1]), entry.getValue()));
        }
        return clusters;
    }
}
