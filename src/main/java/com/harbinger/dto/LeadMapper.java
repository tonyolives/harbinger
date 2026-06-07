package com.harbinger.dto;

import com.harbinger.model.Lead;

/**
 * Maps the domain {@link Lead} to its API {@link LeadDto}. Stateless and static — keeps the
 * controller and SSE publisher thin and the mapping in one place.
 */
public final class LeadMapper {

    private LeadMapper() {
    }

    public static LeadDto toDto(Lead lead) {
        if (lead == null) {
            throw new IllegalArgumentException("lead must not be null");
        }
        return new LeadDto(
                lead.homeowner().id(),
                lead.homeowner().name(),
                lead.homeowner().address(),
                lead.intentScore(),
                lead.tier(),
                lead.reasons(),
                lead.explanation(),
                lead.signalToLeadMs(),
                lead.surfacedAt());
    }
}
