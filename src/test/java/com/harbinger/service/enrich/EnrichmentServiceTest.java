package com.harbinger.service.enrich;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.EnrichedHomeowner;
import com.harbinger.model.Homeowner;
import com.harbinger.model.PropertyDetails;
import com.harbinger.model.RawSignal;
import com.harbinger.service.SignalGeneratorService;
import com.harbinger.service.ingest.AddressNormalizer;
import com.harbinger.service.ingest.NameNormalizer;
import com.harbinger.service.resolution.ResolutionService;
import com.harbinger.service.resolution.ResolvedCluster;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EnrichmentServiceTest {

    private static final Clock FIXED =
            Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);

    private final EnrichmentService enrichment = new EnrichmentService();

    private static Homeowner homeowner(String name, String address) {
        // Mirror how ResolutionService mints ids: a deterministic hash of name|address.
        UUID id = UUID.nameUUIDFromBytes((name + "|" + address).getBytes());
        return new Homeowner(id, name, address);
    }

    @Test
    void sameHomeownerEnrichesIdentically() {
        Homeowner owner = homeowner("john smith", "123 main st");

        assertThat(enrichment.enrich(owner)).isEqualTo(enrichment.enrich(owner));
    }

    @Test
    void differentHomeownersGetDifferentProperties() {
        PropertyDetails a = enrichment.enrich(homeowner("john smith", "123 main st")).property();
        PropertyDetails b = enrichment.enrich(homeowner("jane doe", "987 oak ave")).property();

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void propertyIsKeyedOnParcelWhileContactIsKeyedOnPerson() {
        // A resolution miss splits one owner into "bob" and "robert" at the same address.
        EnrichedHomeowner bob = enrichment.enrich(homeowner("bob smith", "123 main st"));
        EnrichedHomeowner robert = enrichment.enrich(homeowner("robert smith", "123 main st"));

        // Same parcel → the same house facts, regardless of the name queried.
        assertThat(bob.property()).isEqualTo(robert.property());
        // But contact is person-specific, so the split surfaces as diverging contacts.
        assertThat(bob.contact()).isNotEqualTo(robert.contact());
        assertThat(bob.contact().email()).isEqualTo("bob.smith@example.invalid");
        assertThat(robert.contact().email()).isEqualTo("robert.smith@example.invalid");
    }

    @Test
    void contactIsObviouslyFakeAndFlaggedMock() {
        EnrichedHomeowner enriched = enrichment.enrich(homeowner("john smith", "123 main st"));

        assertThat(enriched.contact().phone()).matches("^555-01\\d{2}$");
        assertThat(enriched.contact().email()).isEqualTo("john.smith@example.invalid");
        assertThat(enriched.contact().mock()).isTrue();
    }

    @Test
    void emailIsDerivedFromTheHomeownerName() {
        EnrichedHomeowner enriched = enrichment.enrich(homeowner("mary jane watson", "5 elm st"));

        assertThat(enriched.contact().email()).isEqualTo("mary.jane.watson@example.invalid");
    }

    @Test
    void propertyFieldsFallWithinDeclaredRanges() {
        PropertyDetails p = enrichment.enrich(homeowner("john smith", "123 main st")).property();

        assertThat(p.yearBuilt()).isBetween(1950, 2020);
        assertThat(p.beds()).isBetween(2, 5);
        assertThat(p.baths()).isBetween(1, 4);
        assertThat(p.lotSizeSqFt()).isBetween(3_000, 12_000);
        assertThat(p.estimatedMarketValue()).isBetween(200_000L, 1_200_000L);
    }

    @Test
    void rejectsNullHomeowner() {
        assertThatThrownBy(() -> enrichment.enrich(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("homeowner");
    }

    /**
     * The "equity never negative-by-bug" guarantee, exercised against real resolved
     * homeowners rather than hand-picked values: over a noisy multi-owner run, every
     * enriched property keeps {@code mortgage <= market} and {@code equity == market - mortgage}.
     */
    @Test
    void equityIsNeverNegativeAcrossManyResolvedHomeowners() {
        SignalGeneratorService generator = new SignalGeneratorService(FIXED);
        ResolutionService resolver =
                new ResolutionService(new NameNormalizer(), new AddressNormalizer());

        for (long seed : List.of(1L, 7L, 13L, 42L, 99L, 2024L)) {
            List<RawSignal> signals = generator.generate(seed, 12, 8);
            for (ResolvedCluster cluster : resolver.resolve(signals)) {
                PropertyDetails p = enrichment.enrich(cluster.homeowner()).property();

                assertThat(p.equity()).isGreaterThanOrEqualTo(0L);
                assertThat(p.mortgageBalance()).isLessThanOrEqualTo(p.estimatedMarketValue());
                assertThat(p.equity()).isEqualTo(p.estimatedMarketValue() - p.mortgageBalance());
            }
        }
    }
}
