package com.harbinger.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.Homeowner;
import com.harbinger.model.Lead;
import com.harbinger.model.Tier;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LeadMapperTest {

    @Test
    void mapsEveryField() {
        Homeowner owner = new Homeowner(UUID.randomUUID(), "john smith", "123 main st");
        Instant surfaced = Instant.parse("2026-06-06T00:00:00Z");
        Lead lead = new Lead(owner, 80, Tier.HOT, List.of("Pre-foreclosure filing"),
                "why this lead", 5L, surfaced);

        LeadDto dto = LeadMapper.toDto(lead);

        assertThat(dto.homeownerId()).isEqualTo(owner.id());
        assertThat(dto.name()).isEqualTo("john smith");
        assertThat(dto.address()).isEqualTo("123 main st");
        assertThat(dto.intentScore()).isEqualTo(80);
        assertThat(dto.tier()).isEqualTo(Tier.HOT);
        assertThat(dto.reasons()).containsExactly("Pre-foreclosure filing");
        assertThat(dto.explanation()).isEqualTo("why this lead");
        assertThat(dto.signalToLeadMs()).isEqualTo(5L);
        assertThat(dto.surfacedAt()).isEqualTo(surfaced);
    }

    @Test
    void nullLeadRejected() {
        assertThatThrownBy(() -> LeadMapper.toDto(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
