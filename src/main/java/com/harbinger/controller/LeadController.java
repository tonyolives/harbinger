package com.harbinger.controller;

import com.harbinger.dto.LeadDto;
import com.harbinger.dto.LeadMapper;
import com.harbinger.dto.MetricsDto;
import com.harbinger.model.Lead;
import com.harbinger.repository.LeadRepository;
import com.harbinger.service.pipeline.RealtimeLeadService;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Read API over the surfaced leads plus the live SSE stream. Thin by design: ranking and storage
 * live in {@link LeadRepository}, the realtime numbers in {@link RealtimeLeadService}, and the SSE
 * plumbing in {@link SseLeadEventPublisher}; this class only shapes requests into DTOs.
 */
@RestController
@RequestMapping("/api/v1")
public class LeadController {

    private final LeadRepository repository;
    private final RealtimeLeadService realtimeLeadService;
    private final SseLeadEventPublisher publisher;

    public LeadController(
            LeadRepository repository,
            RealtimeLeadService realtimeLeadService,
            SseLeadEventPublisher publisher) {
        this.repository = repository;
        this.realtimeLeadService = realtimeLeadService;
        this.publisher = publisher;
    }

    /** All surfaced leads, strongest first. */
    @GetMapping("/leads")
    public List<LeadDto> leads() {
        return repository.findAllRanked().stream().map(LeadMapper::toDto).toList();
    }

    /** One lead by homeowner id, or 404. */
    @GetMapping("/leads/{id}")
    public LeadDto lead(@PathVariable UUID id) {
        Lead lead = repository.findById(id).orElseThrow(() -> new LeadNotFoundException(id));
        return LeadMapper.toDto(lead);
    }

    /** Live pipeline metrics, including signal-to-lead latency percentiles. */
    @GetMapping("/metrics")
    public MetricsDto metrics() {
        List<Long> latencies = repository.findAllRanked().stream()
                .map(Lead::signalToLeadMs)
                .toList();
        return MetricsDto.of(
                realtimeLeadService.signalsProcessed(),
                realtimeLeadService.leadsSurfaced(),
                realtimeLeadService.tierCounts(),
                latencies);
    }

    /** Server-Sent Events: a {@code lead} event each time a lead surfaces. */
    @GetMapping("/stream")
    public SseEmitter stream() {
        return publisher.subscribe();
    }
}
