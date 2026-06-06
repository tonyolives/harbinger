package com.harbinger.service.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.Homeowner;
import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

/**
 * Tests for the pairwise precision/recall/F1 measurement. Clusters are hand-built with
 * controlled {@code trueOwnerId}s so the expected metrics are computable by hand. Tagged
 * as the sanctioned ground-truth reader.
 */
@Tag("evaluation")
class ResolutionServiceEvaluateTest {

    private static final Instant AT = Instant.parse("2026-06-01T12:00:00Z");
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final Homeowner ANY = new Homeowner(
            UUID.fromString("00000000-0000-0000-0000-000000000001"), "n", "a");

    private final ResolutionService service =
            new ResolutionService(new NameNormalizer(), new AddressNormalizer());

    private static RawSignal signal(UUID trueOwnerId) {
        return new RawSignal("n", "a", Source.COURT, SignalType.PROBATE, AT, trueOwnerId);
    }

    /** A cluster whose members carry the given true owners, in order. */
    private static ResolvedCluster cluster(UUID... owners) {
        List<RawSignal> signals = Arrays.stream(owners).map(ResolutionServiceEvaluateTest::signal).toList();
        return new ResolvedCluster(UUID.randomUUID(), ANY, signals);
    }

    @Test
    void perfectClusteringScoresOne() {
        ResolutionMetrics m = service.evaluate(List.of(
                cluster(A, A, A),
                cluster(B, B)));

        assertThat(m.precision()).isEqualTo(1.0);
        assertThat(m.recall()).isEqualTo(1.0);
        assertThat(m.f1()).isEqualTo(1.0);
    }

    @Test
    void overMergingLowersPrecisionNotRecall() {
        // One cluster wrongly mixes owner A (x2) and owner B (x1).
        ResolutionMetrics m = service.evaluate(List.of(cluster(A, A, B)));

        // predicted pairs = 3, true-positive pairs = 1 (the A,A pair), true pairs = 1.
        assertThat(m.precision()).isEqualTo(1.0 / 3);
        assertThat(m.recall()).isEqualTo(1.0);
    }

    @Test
    void underMergingLowersRecallNotPrecision() {
        // Owner A's three signals are split across two clusters.
        ResolutionMetrics m = service.evaluate(List.of(
                cluster(A, A),
                cluster(A)));

        // predicted pairs = 1, true-positive pairs = 1, true pairs = 3.
        assertThat(m.precision()).isEqualTo(1.0);
        assertThat(m.recall()).isEqualTo(1.0 / 3);
    }

    @Test
    void computesExactPairCountsAndF1() {
        // cluster1 = [A, A, B], cluster2 = [A].
        // predicted = C(3,2) = 3; TP = C(2,2 of A) = 1; true = C(3,2 of A) = 3.
        ResolutionMetrics m = service.evaluate(List.of(
                cluster(A, A, B),
                cluster(A)));

        assertThat(m.predictedPairs()).isEqualTo(3);
        assertThat(m.truePositivePairs()).isEqualTo(1);
        assertThat(m.truePairs()).isEqualTo(3);
        assertThat(m.precision()).isEqualTo(1.0 / 3);
        assertThat(m.recall()).isEqualTo(1.0 / 3);
        assertThat(m.f1()).isEqualTo(1.0 / 3);
    }

    @Test
    void allSingletonsScoreOne() {
        // No pairs exist anywhere, so there is nothing to get wrong.
        ResolutionMetrics m = service.evaluate(List.of(cluster(A), cluster(B)));

        assertThat(m.precision()).isEqualTo(1.0);
        assertThat(m.recall()).isEqualTo(1.0);
        assertThat(m.f1()).isEqualTo(1.0);
    }

    @Test
    void emptyRunScoresOne() {
        ResolutionMetrics m = service.evaluate(List.of());

        assertThat(m.f1()).isEqualTo(1.0);
    }

    @Test
    void rejectsNullClusters() {
        assertThatThrownBy(() -> service.evaluate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
