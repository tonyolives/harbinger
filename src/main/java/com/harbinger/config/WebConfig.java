package com.harbinger.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web layer config: allow the Vite dev UI (Phase 8, {@code http://localhost:5173}) to call the
 * read API, open the SSE stream, and drive the demo. {@code GET} covers the read endpoints and the
 * stream; {@code POST} covers the demo control endpoints ({@code /demo/start}, {@code /demo/reset});
 * {@code OPTIONS} covers any CORS preflight.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Origin the local React/Vite dev server runs on. */
    static final String UI_ORIGIN = "http://localhost:5173";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(UI_ORIGIN)
                .allowedMethods("GET", "POST", "OPTIONS");
    }
}
