package com.harbinger.controller;

import com.harbinger.dto.LeadMapper;
import com.harbinger.model.Lead;
import com.harbinger.service.pipeline.LeadEventPublisher;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * The SSE-backed {@link LeadEventPublisher}. Browsers subscribe to {@code /api/v1/stream} and
 * receive a named {@code lead} event (a {@link com.harbinger.dto.LeadDto}) the instant a lead
 * surfaces. Being a real bean, it replaces the no-op fallback from {@code AppConfig} via
 * {@code @ConditionalOnMissingBean}.
 *
 * <p>Emitters are held in a {@link CopyOnWriteArrayList} (safe to iterate while the orchestrator
 * thread publishes); dead ones are dropped on completion, timeout, error, or a failed send.
 */
@Service
public class SseLeadEventPublisher implements LeadEventPublisher {

    /** Keep streams open for an hour of idle before the client must reconnect. */
    private static final long STREAM_TIMEOUT_MS = 3_600_000L;

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /** Register a new subscriber and return its emitter for the controller to hand back. */
    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(STREAM_TIMEOUT_MS);
        register(emitter);
        return emitter;
    }

    /** Wire removal callbacks and track the emitter (package-private so it's unit-testable). */
    void register(SseEmitter emitter) {
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        emitters.add(emitter);
    }

    @Override
    public void publish(Lead lead) {
        SseEmitter.SseEventBuilder event = SseEmitter.event().name("lead").data(LeadMapper.toDto(lead));
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(event);
            } catch (IOException | IllegalStateException ex) {
                emitters.remove(emitter);
            }
        }
    }

    /** Current subscriber count — for tests and operational visibility. */
    public int subscriberCount() {
        return emitters.size();
    }
}
