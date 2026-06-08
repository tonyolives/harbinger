package com.harbinger.config;

import com.harbinger.service.pipeline.LeadEventPublisher;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Shared infrastructure beans. The {@link Clock} is injected wherever code needs
 * "now" so timestamps stay deterministic and testable (tests pass a fixed clock).
 */
@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    /**
     * Fallback {@link LeadEventPublisher} so the orchestrator wires before the web layer exists.
     * The SSE-backed publisher (added with the controller) replaces it via
     * {@link ConditionalOnMissingBean}.
     */
    @Bean
    @ConditionalOnMissingBean(LeadEventPublisher.class)
    public LeadEventPublisher noOpLeadEventPublisher() {
        return lead -> { /* no subscribers until the SSE publisher is wired in */ };
    }
}
