package com.harbinger.model;

/**
 * A resolved {@link Homeowner} with the enrichment step's additions: synthetic
 * {@link PropertyDetails} and a mock {@link ContactInfo}. The homeowner identity is
 * left untouched — enrichment is a separate layer wrapped around it, not a mutation of
 * it — so resolution stays decoupled from enrichment. Scoring (Phase 5) reads these.
 */
public record EnrichedHomeowner(
        Homeowner homeowner,
        PropertyDetails property,
        ContactInfo contact
) {
    public EnrichedHomeowner {
        if (homeowner == null) {
            throw new IllegalArgumentException("homeowner must not be null");
        }
        if (property == null) {
            throw new IllegalArgumentException("property must not be null");
        }
        if (contact == null) {
            throw new IllegalArgumentException("contact must not be null");
        }
    }
}
