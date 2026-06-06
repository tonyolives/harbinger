package com.harbinger.service.enrich;

import com.harbinger.model.ContactInfo;
import com.harbinger.model.EnrichedHomeowner;
import com.harbinger.model.Homeowner;
import com.harbinger.model.PropertyDetails;
import com.harbinger.model.PropertyType;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * Mocks the "enrich + skip-trace" step: given a resolved {@link Homeowner}, attaches
 * synthetic {@link PropertyDetails} and an obviously-fake {@link ContactInfo}.
 *
 * <p>Everything is derived deterministically from the homeowner's own synthetic id
 * (itself a stable hash of the resolved name and address), so the same homeowner always
 * enriches the same way — no clock, no I/O, no network. A plain seeded
 * {@link java.util.Random} keeps the derivation simple enough to read top to bottom;
 * the order of draws below is therefore load-bearing and must not be reordered.
 *
 * <p>No real data is ever consulted: the phone uses the reserved fictional
 * {@code 555-01xx} range, the email the {@code .invalid} TLD, and the contact is flagged
 * {@code mock = true}.
 */
@Service
public class EnrichmentService {

    /** Email domain that can never resolve — RFC 6761 reserves {@code .invalid}. */
    private static final String MOCK_EMAIL_DOMAIN = "example.invalid";

    public EnrichedHomeowner enrich(Homeowner homeowner) {
        if (homeowner == null) {
            throw new IllegalArgumentException("homeowner must not be null");
        }

        // Seed from the homeowner's deterministic id so enrichment is reproducible.
        long seed = homeowner.id().getMostSignificantBits()
                ^ homeowner.id().getLeastSignificantBits();
        Random rng = new Random(seed);

        PropertyDetails property = buildProperty(rng);
        ContactInfo contact = buildContact(rng, homeowner.name());
        return new EnrichedHomeowner(homeowner, property, contact);
    }

    private static PropertyDetails buildProperty(Random rng) {
        PropertyType propertyType = PropertyType.values()[rng.nextInt(PropertyType.values().length)];
        int yearBuilt = 1950 + rng.nextInt(71); // 1950–2020
        int beds = 2 + rng.nextInt(4); // 2–5
        int baths = 1 + rng.nextInt(4); // 1–4
        int lotSizeSqFt = 3_000 + rng.nextInt(9_001); // 3,000–12,000

        long estimatedMarketValue = 200_000L + rng.nextInt(1_000_001); // 200k–1.2M
        // Assessed value runs a touch under market: 75–95% of it.
        long assessedValue = Math.round(estimatedMarketValue * (0.75 + rng.nextDouble() * 0.20));
        // Draw the mortgage as a fraction (0–90%) of market value, so equity — market
        // minus mortgage — is non-negative by construction, not by a clamp after a bug.
        long mortgageBalance = Math.round(estimatedMarketValue * (rng.nextDouble() * 0.90));
        long equity = Math.max(0, estimatedMarketValue - mortgageBalance);

        return new PropertyDetails(
                propertyType, yearBuilt, beds, baths, lotSizeSqFt,
                assessedValue, estimatedMarketValue, mortgageBalance, equity);
    }

    private static ContactInfo buildContact(Random rng, String name) {
        // Reserved fictional range 555-0100 … 555-0199 — never a real subscriber line.
        String phone = String.format("555-01%02d", rng.nextInt(100));
        // Name is already normalized lowercase (e.g. "john smith") → "john.smith@example.invalid".
        String localPart = name.trim().replaceAll("\\s+", ".");
        String email = localPart + "@" + MOCK_EMAIL_DOMAIN;
        return new ContactInfo(phone, email, true);
    }
}
