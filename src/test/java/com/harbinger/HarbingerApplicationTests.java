package com.harbinger;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Pin the opt-in ClaudeLlmProvider off (it's @ConditionalOnProperty on anthropic.api-key) so
// the booted DemoRunner uses MockLlmProvider and never calls the network — even on a developer
// machine that exports ANTHROPIC_API_KEY. Honors AGENTS rule: no network in tests.
@SpringBootTest
@TestPropertySource(properties = "anthropic.api-key=false")
class HarbingerApplicationTests {

	@Test
	void contextLoads() {
	}

}
