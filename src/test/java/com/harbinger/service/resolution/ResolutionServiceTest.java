package com.harbinger.service.resolution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.RawSignal;
import com.harbinger.model.Source;
import com.harbinger.model.SignalType;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the resolve path. Signals carry an arbitrary {@code trueOwnerId}
 * the service must ignore — resolution accuracy is measured separately by
 * {@code evaluate()} and the integration test.
 */
class ResolutionServiceTest {

    private static final Instant AT = Instant.parse("2026-06-01T12:00:00Z");

    private final ResolutionService service =
            new ResolutionService(new NameNormalizer(), new AddressNormalizer());

    private static RawSignal signal(String name, String address) {
        // trueOwnerId is random on purpose: resolve() must not depend on it.
        return new RawSignal(name, address, Source.COURT, SignalType.PROBATE, AT, UUID.randomUUID());
    }

    @Test
    void samePersonDifferentSpellingMerges() {
        List<ResolvedCluster> clusters = service.resolve(List.of(
                signal("John Smith", "123 Main Street"),
                signal("J Smith", "123 Main St"),
                signal("Smith, John", "123 MAIN STREET")));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).signals()).hasSize(3);
        // Representative favors the full normalized form over the initial-only one.
        assertThat(clusters.get(0).homeowner().name()).isEqualTo("john smith");
        assertThat(clusters.get(0).homeowner().address()).isEqualTo("123 main st");
    }

    @Test
    void differentPeopleSameAddressDoNotMerge() {
        List<ResolvedCluster> clusters = service.resolve(List.of(
                signal("John Smith", "123 Main St"),
                signal("Mary Jones", "123 Main Street")));

        assertThat(clusters).hasSize(2);
        assertThat(clusters).allSatisfy(c -> assertThat(c.signals()).hasSize(1));
    }

    @Test
    void sameLastNameDifferentFirstAtOneAddressDoNotMerge() {
        // Precision guard: a shared surname at one address is not enough to merge.
        List<ResolvedCluster> clusters = service.resolve(List.of(
                signal("John Smith", "123 Main St"),
                signal("Mary Smith", "123 Main Street")));

        assertThat(clusters).hasSize(2);
    }

    @Test
    void singleTokenNamesFallBackToWholeStringMatch() {
        List<ResolvedCluster> clusters = service.resolve(List.of(
                signal("Cher", "1 Star Blvd"),
                signal("cher", "1 Star Boulevard")));

        assertThat(clusters).hasSize(1);
        assertThat(clusters.get(0).signals()).hasSize(2);
    }

    @Test
    void cleanMultiOwnerSetYieldsOneClusterEach() {
        List<ResolvedCluster> clusters = service.resolve(List.of(
                signal("John Smith", "123 Main St"),
                signal("Mary Jones", "456 Oak Ave"),
                signal("Bob Lee", "789 Pine Rd")));

        assertThat(clusters).hasSize(3);
    }

    @Test
    void clusterIdIsDeterministicForTheSameResolvedIdentity() {
        UUID first = service.resolve(List.of(signal("John Smith", "123 Main Street")))
                .get(0).id();
        UUID second = service.resolve(List.of(signal("john smith", "123 Main St")))
                .get(0).id();

        assertThat(first).isEqualTo(second);
    }

    @Test
    void emptyInputYieldsNoClusters() {
        assertThat(service.resolve(List.of())).isEmpty();
    }

    @Test
    void rejectsNullInput() {
        assertThatThrownBy(() -> service.resolve(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
