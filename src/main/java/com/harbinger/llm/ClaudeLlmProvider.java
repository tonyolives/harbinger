package com.harbinger.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.harbinger.service.scoring.Score;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * The opt-in {@link LlmProvider}: asks the Claude Messages API for the "why this lead" line
 * instead of building it from a template. Selected only when {@code ANTHROPIC_API_KEY} is set
 * — Spring Boot relaxed binding maps that env var to the {@code anthropic.api-key} property, so
 * the {@link ConditionalOnProperty} below keeps this bean (and, being {@link Primary}, the one
 * that wins) out of the context entirely when no key is present. That leaves
 * {@link MockLlmProvider} as the sole provider, which is why {@code ./mvnw verify} never needs
 * a key or the network.
 *
 * <p>It calls {@code POST https://api.anthropic.com/v1/messages} with a tight token budget and
 * a system prompt that asks for at most two sentences. The {@link RestClient} is built from the
 * injected builder once in the constructor; taking the builder (rather than a finished client)
 * is what lets the test bind a {@code MockRestServiceServer} to it and exercise this class with
 * no real HTTP.
 */
@Service
@Primary
@ConditionalOnProperty(prefix = "anthropic", name = "api-key")
public class ClaudeLlmProvider implements LlmProvider {

    private static final String BASE_URL = "https://api.anthropic.com";
    private static final String MESSAGES_PATH = "/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /** Cheapest/fastest model — plenty for a two-sentence explanation. */
    private static final String MODEL = "claude-haiku-4-5";
    private static final int MAX_TOKENS = 150;
    private static final String SYSTEM_PROMPT =
            "You explain why a homeowner is a likely property-seller lead. Using only the "
                    + "supplied tier, score, and reasons, write at most two plain sentences. "
                    + "No preamble, no markdown, no restating the inputs verbatim.";

    private final RestClient restClient;
    private final String apiKey;

    public ClaudeLlmProvider(RestClient.Builder builder, @Value("${anthropic.api-key}") String apiKey) {
        this.restClient = builder.baseUrl(BASE_URL).build();
        this.apiKey = apiKey;
    }

    @Override
    public String explain(Score score) {
        if (score == null) {
            throw new IllegalArgumentException("score must not be null");
        }
        if (score.reasons().isEmpty()) {
            throw new IllegalArgumentException("score must carry at least one reason");
        }

        AnthropicRequest request = new AnthropicRequest(
                MODEL, MAX_TOKENS, SYSTEM_PROMPT, List.of(new Message("user", userContent(score))));

        AnthropicResponse response = restClient.post()
                .uri(MESSAGES_PATH)
                .header("x-api-key", apiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);

        if (response == null || response.content() == null || response.content().isEmpty()) {
            throw new IllegalStateException("Claude returned no content");
        }
        return response.content().get(0).text().trim();
    }

    /** Render the score as the user turn the model reasons over. */
    private static String userContent(Score score) {
        return "Tier: " + score.tier() + " (intent score " + score.value() + ")."
                + " Reasons (strongest first): " + String.join("; ", score.reasons()) + ".";
    }

    // Minimal request/response shapes for the Messages API. Spring Boot's Jackson ignores
    // unknown response fields (id, role, usage, ...), so we model only what we send and read.

    private record Message(String role, String content) {}

    private record AnthropicRequest(
            String model, @JsonProperty("max_tokens") int maxTokens, String system, List<Message> messages) {}

    private record TextBlock(String type, String text) {}

    private record AnthropicResponse(List<TextBlock> content) {}
}
