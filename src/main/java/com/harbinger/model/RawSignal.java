package com.harbinger.model;

import java.time.Instant;
import java.util.UUID;

/**
 * A single raw public-record signal as it first arrives deliberately messy.
 * {@code rawName} and {@code rawAddress} are unnormalized strings (e.g.
 * "J Smith" / "123 Main Street"); later phases clean and resolve them.
 *
 * <p>{@code trueOwnerId} is a ground-truth label: every messy signal belonging
 * to the same synthetic homeowner shares one id. It exists only so tests and the
 * benchmark can measure resolution accuracy. Production pipeline code
 * (services/controllers/repositories) must never read it.
 */
public record RawSignal(
        String rawName,
        String rawAddress,
        Source source,
        SignalType signalType,
        Instant observedAt,
        UUID trueOwnerId
) {
    public RawSignal {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("rawName must not be blank");
        }
        if (rawAddress == null || rawAddress.isBlank()) {
            throw new IllegalArgumentException("rawAddress must not be blank");
        }
        if (source == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (signalType == null) {
            throw new IllegalArgumentException("signalType must not be null");
        }
        if (observedAt == null) {
            throw new IllegalArgumentException("observedAt must not be null");
        }
        if (trueOwnerId == null) {
            throw new IllegalArgumentException("trueOwnerId must not be null");
        }
    }
}
