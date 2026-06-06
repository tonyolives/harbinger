package com.harbinger.model;

import java.util.UUID;

/**
 * A resolved homeowner identity — the canonical person several messy
 * {@link RawSignal}s collapse into. In Phase 1 it is the clean seed the
 * generator perturbs; later phases produce it via entity resolution.
 * Property and contact details are added in Phase 4.
 */
public record Homeowner(
        UUID id,
        String name,
        String address
) {
    public Homeowner {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address must not be blank");
        }
    }
}
