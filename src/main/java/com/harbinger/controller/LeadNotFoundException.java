package com.harbinger.controller;

import java.util.UUID;

/**
 * Thrown when a lead is requested by a homeowner id that has not surfaced. Mapped to HTTP 404 by
 * {@link GlobalExceptionHandler}.
 */
public class LeadNotFoundException extends RuntimeException {

    public LeadNotFoundException(UUID homeownerId) {
        super("No lead for homeowner " + homeownerId);
    }
}
