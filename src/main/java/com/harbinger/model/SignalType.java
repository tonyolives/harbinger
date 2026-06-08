package com.harbinger.model;

/**
 * The kind of seller-intent signal a record represents. DEED_TRANSFER is a
 * negative signal (the home likely just changed hands) and dampens scoring in
 * Phase 5; the rest are positive motivation signals.
 */
public enum SignalType {
    PRE_FORECLOSURE,
    TAX_DELINQUENCY,
    PROBATE,
    DIVORCE,
    EVICTION,
    DEED_TRANSFER
}
