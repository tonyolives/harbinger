package com.harbinger.model;

/**
 * Synthetic property facts the enrichment step attaches to a resolved
 * {@link Homeowner}. Every field is derived deterministically from the homeowner's
 * identity, so the same homeowner always yields the same details — no real
 * assessor data is ever read.
 *
 * <p>{@code equity} is {@code estimatedMarketValue - mortgageBalance}, never below
 * zero. The compact constructor asserts that invariant so a property can never be
 * built with negative equity by a bug upstream. Monetary fields are whole dollars.
 */
public record PropertyDetails(
        PropertyType propertyType,
        int yearBuilt,
        int beds,
        int baths,
        int lotSizeSqFt,
        long assessedValue,
        long estimatedMarketValue,
        long mortgageBalance,
        long equity
) {
    public PropertyDetails {
        if (propertyType == null) {
            throw new IllegalArgumentException("propertyType must not be null");
        }
        if (yearBuilt <= 0) {
            throw new IllegalArgumentException("yearBuilt must be positive");
        }
        if (beds <= 0) {
            throw new IllegalArgumentException("beds must be positive");
        }
        if (baths <= 0) {
            throw new IllegalArgumentException("baths must be positive");
        }
        if (lotSizeSqFt <= 0) {
            throw new IllegalArgumentException("lotSizeSqFt must be positive");
        }
        if (assessedValue < 0) {
            throw new IllegalArgumentException("assessedValue must not be negative");
        }
        if (estimatedMarketValue < 0) {
            throw new IllegalArgumentException("estimatedMarketValue must not be negative");
        }
        if (mortgageBalance < 0) {
            throw new IllegalArgumentException("mortgageBalance must not be negative");
        }
        if (equity < 0) {
            throw new IllegalArgumentException("equity must not be negative");
        }
    }
}
