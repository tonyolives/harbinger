package com.harbinger.config;

import java.time.Clock;
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
}
