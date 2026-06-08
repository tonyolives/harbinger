package com.harbinger.dto;

import com.harbinger.model.Tier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The API view of a surfaced lead — decoupled from the domain {@code Lead} so the wire shape can
 * evolve independently. Flattens the homeowner's identity onto the lead and carries the Phase 6
 * explanation plus the north-star {@code signalToLeadMs}.
 */
public record LeadDto(
        UUID homeownerId,
        String name,
        String address,
        int intentScore,
        Tier tier,
        List<String> reasons,
        String explanation,
        long signalToLeadMs,
        Instant surfacedAt) {
}
