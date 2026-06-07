package com.harbinger.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.harbinger.model.Homeowner;
import com.harbinger.model.Lead;
import com.harbinger.model.Tier;
import com.harbinger.repository.LeadRepository;
import com.harbinger.service.pipeline.RealtimeLeadService;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * MockMvc coverage of the read API: ranked leads, a single lead (200 and 404 via the advice),
 * the metrics snapshot, and that the SSE stream opens as {@code text/event-stream}. All
 * collaborators are mocked — no pipeline, no network.
 */
@WebMvcTest(LeadController.class)
class LeadControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private LeadRepository repository;
    @MockBean
    private RealtimeLeadService realtimeLeadService;
    @MockBean
    private SseLeadEventPublisher publisher;

    private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private static Lead lead(UUID id, int score, Tier tier) {
        Homeowner owner = new Homeowner(id, "john smith", "123 main st");
        return new Lead(owner, score, tier, List.of("Pre-foreclosure filing"),
                "why this lead", 5L, Instant.parse("2026-06-06T00:00:00Z"));
    }

    @Test
    void leadsReturnsRankedJson() throws Exception {
        when(repository.findAllRanked()).thenReturn(List.of(
                lead(UUID.randomUUID(), 80, Tier.HOT),
                lead(UUID.randomUUID(), 75, Tier.HOT)));

        mvc.perform(get("/api/v1/leads"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].intentScore").value(80))
                .andExpect(jsonPath("$[0].explanation").value("why this lead"))
                .andExpect(jsonPath("$[1].intentScore").value(75));
    }

    @Test
    void leadByIdReturnsLead() throws Exception {
        when(repository.findById(ID)).thenReturn(Optional.of(lead(ID, 90, Tier.HOT)));

        mvc.perform(get("/api/v1/leads/{id}", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.homeownerId").value(ID.toString()))
                .andExpect(jsonPath("$.tier").value("HOT"))
                .andExpect(jsonPath("$.signalToLeadMs").value(5));
    }

    @Test
    void leadByIdReturns404WhenMissing() throws Exception {
        when(repository.findById(any())).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/leads/{id}", ID))
                .andExpect(status().isNotFound());
    }

    @Test
    void metricsReturnsSnapshot() throws Exception {
        when(realtimeLeadService.signalsProcessed()).thenReturn(12);
        when(realtimeLeadService.leadsSurfaced()).thenReturn(2);
        when(realtimeLeadService.tierCounts())
                .thenReturn(Map.of(Tier.HOT, 2L, Tier.WARM, 1L, Tier.COLD, 3L));
        when(repository.findAllRanked()).thenReturn(List.of(
                lead(UUID.randomUUID(), 80, Tier.HOT),
                lead(UUID.randomUUID(), 75, Tier.HOT)));

        mvc.perform(get("/api/v1/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.signalsProcessed").value(12))
                .andExpect(jsonPath("$.leadsSurfaced").value(2))
                .andExpect(jsonPath("$.hotCount").value(2))
                .andExpect(jsonPath("$.warmCount").value(1))
                .andExpect(jsonPath("$.coldCount").value(3))
                .andExpect(jsonPath("$.signalToLeadP50Ms").value(5));
    }

    @Test
    void streamOpensAsEventStream() throws Exception {
        when(publisher.subscribe()).thenReturn(new SseEmitter());

        mvc.perform(get("/api/v1/stream"))
                .andExpect(status().isOk())
                .andExpect(request().asyncStarted());

        verify(publisher).subscribe();
    }
}
