package com.harbinger.llm;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Asserts the conditional wiring without touching the network: with no {@code anthropic.api-key}
 * the only {@link LlmProvider} is {@link MockLlmProvider}; with the key set, the {@code @Primary}
 * {@link ClaudeLlmProvider} is the one injected. Constructing the Claude bean does no HTTP, so
 * this stays offline.
 */
class LlmProviderWiringTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestClientAutoConfiguration.class))
            .withUserConfiguration(MockLlmProvider.class, ClaudeLlmProvider.class);

    @Test
    void withoutApiKeyTheDefaultIsMock() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LlmProvider.class);
            assertThat(context.getBean(LlmProvider.class)).isInstanceOf(MockLlmProvider.class);
        });
    }

    @Test
    void withApiKeyTheClaudeProviderIsPrimary() {
        runner.withPropertyValues("anthropic.api-key=test-key").run(context -> {
            assertThat(context).hasBean("mockLlmProvider");
            assertThat(context).hasBean("claudeLlmProvider");
            // @Primary resolves the by-type lookup to the Claude provider.
            assertThat(context.getBean(LlmProvider.class)).isInstanceOf(ClaudeLlmProvider.class);
        });
    }
}
