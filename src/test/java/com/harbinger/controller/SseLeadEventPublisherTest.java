package com.harbinger.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.harbinger.model.Homeowner;
import com.harbinger.model.Lead;
import com.harbinger.model.Tier;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Covers the SSE publisher offline: subscribers are tracked, leads are sent, and dead emitters
 * (completed / timed-out / errored / failed send) are dropped. No servlet container — emitters
 * are real for the happy path and mocked where a failure must be simulated.
 */
class SseLeadEventPublisherTest {

    private final SseLeadEventPublisher publisher = new SseLeadEventPublisher();

    private static Lead lead() {
        Homeowner owner = new Homeowner(UUID.randomUUID(), "john smith", "123 main st");
        return new Lead(owner, 80, Tier.HOT, List.of("Pre-foreclosure filing"), "why",
                1L, Instant.parse("2026-06-06T00:00:00Z"));
    }

    @Test
    void subscribeTracksAnEmitter() {
        SseEmitter emitter = publisher.subscribe();

        assertThat(emitter).isNotNull();
        assertThat(publisher.subscriberCount()).isEqualTo(1);
    }

    @Test
    void publishSendsToSubscribersWithoutError() {
        publisher.subscribe();

        publisher.publish(lead()); // send buffers on an uninitialized emitter; must not throw

        assertThat(publisher.subscriberCount()).isEqualTo(1);
    }

    @Test
    void publishWithNoSubscribersIsANoOp() {
        publisher.publish(lead());

        assertThat(publisher.subscriberCount()).isZero();
    }

    @Test
    void lifecycleCallbacksRemoveTheEmitter() {
        SseEmitter mockEmitter = mock(SseEmitter.class);
        publisher.register(mockEmitter);
        assertThat(publisher.subscriberCount()).isEqualTo(1);

        ArgumentCaptor<Runnable> onCompletion = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Runnable> onTimeout = ArgumentCaptor.forClass(Runnable.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Consumer<Throwable>> onError = ArgumentCaptor.forClass(Consumer.class);
        verify(mockEmitter).onCompletion(onCompletion.capture());
        verify(mockEmitter).onTimeout(onTimeout.capture());
        verify(mockEmitter).onError(onError.capture());

        onCompletion.getValue().run();
        onTimeout.getValue().run();
        onError.getValue().accept(new RuntimeException("boom"));

        assertThat(publisher.subscriberCount()).isZero();
    }

    @Test
    void failedSendDropsTheEmitter() throws IOException {
        SseEmitter failing = mock(SseEmitter.class);
        doThrow(new IOException("client gone")).when(failing).send(any(SseEmitter.SseEventBuilder.class));
        publisher.register(failing);

        publisher.publish(lead());

        assertThat(publisher.subscriberCount()).isZero();
    }
}
