package com.harbinger.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.harbinger.model.Tier;
import com.harbinger.service.scoring.Score;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Proves the Phase 6 default-provider properties: the explanation is non-empty, deterministic
 * for equal input, at most two sentences, framed by tier and led by the strongest reason, and
 * rejects the same null/empty edges as the other services.
 */
class MockLlmProviderTest {

    private final MockLlmProvider provider = new MockLlmProvider();

    private static Score score(int value, Tier tier, String... reasons) {
        return new Score(value, tier, List.of(reasons));
    }

    /** Non-blank sentences, splitting on terminal punctuation (reason labels carry none). */
    private static long sentenceCount(String text) {
        return java.util.Arrays.stream(text.split("[.!?]+"))
                .filter(s -> !s.isBlank())
                .count();
    }

    @Test
    void producesNonEmptyExplanationFromReasons() {
        String explanation = provider.explain(
                score(78, Tier.HOT, "Pre-foreclosure filing", "Probate (inherited property)"));

        assertThat(explanation).isNotBlank();
        assertThat(explanation).contains("Pre-foreclosure filing");
    }

    @Test
    void outputIsDeterministicForEqualInput() {
        Score a = score(78, Tier.HOT, "Pre-foreclosure filing", "Tax delinquency");
        Score b = score(78, Tier.HOT, "Pre-foreclosure filing", "Tax delinquency");

        assertThat(provider.explain(a)).isEqualTo(provider.explain(b));
    }

    @Test
    void explanationIsAtMostTwoSentences() {
        String explanation = provider.explain(
                score(80, Tier.HOT,
                        "Pre-foreclosure filing", "Probate (inherited property)",
                        "Tax delinquency", "Divorce filing"));

        assertThat(sentenceCount(explanation)).isLessThanOrEqualTo(2);
    }

    @Test
    void framesByTierAndLeadsWithTopReason() {
        assertThat(provider.explain(score(75, Tier.HOT, "Pre-foreclosure filing")))
                .startsWith("HOT lead (score 75): Pre-foreclosure filing")
                .contains("strong intent to sell");
        assertThat(provider.explain(score(45, Tier.WARM, "Divorce filing")))
                .startsWith("WARM lead (score 45): Divorce filing")
                .contains("a possible seller worth watching");
        assertThat(provider.explain(score(10, Tier.COLD, "Eviction filing")))
                .startsWith("COLD lead (score 10): Eviction filing")
                .contains("low intent for now");
    }

    @Test
    void singleReasonHasNoAlsoFlaggedClause() {
        String explanation = provider.explain(score(45, Tier.WARM, "Pre-foreclosure filing"));

        assertThat(explanation).doesNotContain("Also flagged");
        assertThat(sentenceCount(explanation)).isEqualTo(1);
    }

    @Test
    void multipleReasonsListRemainderAfterTheTop() {
        String explanation = provider.explain(
                score(80, Tier.HOT,
                        "Pre-foreclosure filing", "Probate (inherited property)", "Tax delinquency"));

        assertThat(explanation)
                .contains("Also flagged: Probate (inherited property), Tax delinquency");
    }

    @Test
    void nullScoreRejected() {
        assertThatThrownBy(() -> provider.explain(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyReasonsRejected() {
        assertThatThrownBy(() -> provider.explain(new Score(0, Tier.COLD, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
