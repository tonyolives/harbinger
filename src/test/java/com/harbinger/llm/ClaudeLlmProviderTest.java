package com.harbinger.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.harbinger.model.Tier;
import com.harbinger.service.scoring.Score;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * Exercises the opt-in provider with a {@link MockRestServiceServer} bound to the RestClient —
 * no real HTTP. Asserts the request hits the Messages API with the right model, key, and version
 * headers and that the {@code content[0].text} of the response is what gets returned.
 */
class ClaudeLlmProviderTest {

    private static Score score() {
        return new Score(78, Tier.HOT, List.of("Pre-foreclosure filing", "Tax delinquency"));
    }

    @Test
    void callsMessagesApiAndReturnsParsedText() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeLlmProvider provider = new ClaudeLlmProvider(builder, "test-key");

        server.expect(requestTo("https://api.anthropic.com/v1/messages"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-api-key", "test-key"))
                .andExpect(header("anthropic-version", "2023-06-01"))
                .andExpect(jsonPath("$.model").value("claude-haiku-4-5"))
                .andExpect(jsonPath("$.max_tokens").value(150))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Pre-foreclosure filing")))
                .andRespond(withSuccess(
                        "{\"content\":[{\"type\":\"text\",\"text\":\"  Strong seller signals.  \"}]}",
                        MediaType.APPLICATION_JSON));

        String explanation = provider.explain(score());

        assertThat(explanation).isEqualTo("Strong seller signals."); // trimmed
        server.verify();
    }

    @Test
    void nullScoreRejectedBeforeAnyCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeLlmProvider provider = new ClaudeLlmProvider(builder, "test-key");

        assertThatThrownBy(() -> provider.explain(null))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify(); // no request was made
    }

    @Test
    void emptyReasonsRejectedBeforeAnyCall() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        ClaudeLlmProvider provider = new ClaudeLlmProvider(builder, "test-key");

        assertThatThrownBy(() -> provider.explain(new Score(0, Tier.COLD, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
        server.verify();
    }
}
