package com.harbinger.model;

/**
 * The kind of dwelling a {@link Homeowner}'s property is. A small, fixed set the
 * enrichment step assigns deterministically — like {@link Source} or {@link SignalType},
 * it labels synthetic data, not anything scraped.
 */
public enum PropertyType {
    SINGLE_FAMILY,
    CONDO,
    TOWNHOUSE,
    MULTI_FAMILY
}
