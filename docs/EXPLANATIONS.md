# Explanations (Phase 6)

Turns a homeowner's `Score` (value, tier, strongest-first reasons from [Phase 5](SCORING.md))
into a short, human-readable **"why this lead"** line. The work lives in the
`com.harbinger.llm` package behind one interface:

```java
public interface LlmProvider {
    String explain(Score score);   // non-empty, ≤2 sentences
}
```

Keeping it an interface decouples explanation generation from the pipeline and lets the demo
and tests run with no API key and no network, while the real Claude call stays one config flag
away.

## Two implementations

| Bean | When it's used | How it works |
|------|----------------|--------------|
| `MockLlmProvider` | **Default** — always present | Deterministic, tier-aware template built straight from `Score`. No key, no network. |
| `ClaudeLlmProvider` | Opt-in — only when `ANTHROPIC_API_KEY` is set | Calls the Claude Messages API via Spring `RestClient`. `@Primary`, so it wins when present. |

Wiring is a Spring conditional. Spring Boot relaxed binding maps the env var
`ANTHROPIC_API_KEY` → the property `anthropic.api-key`, so `ClaudeLlmProvider` is annotated
`@Primary @ConditionalOnProperty(prefix = "anthropic", name = "api-key")`: it only enters the
context (and only then becomes the primary `LlmProvider`) when the key is present. Otherwise
`MockLlmProvider` is the sole provider — which is why `./mvnw verify` never needs a key.

## Mock template

`MockLlmProvider` is a pure function of the `Score` — no clock, no randomness — so the same
input always yields the same string. Shape:

```
<TIER> lead (score <value>): <top reason> signals <tier phrase>.
[Also flagged: <remaining reasons, comma-separated>.]
```

- **Sentence 1** frames urgency by tier and leads with the strongest reason:
  HOT → "strong intent to sell", WARM → "a possible seller worth watching",
  COLD → "low intent for now".
- **Sentence 2** lists the remaining reasons, and is omitted when there is only one.

Example:

```
HOT lead (score 100): Pre-foreclosure filing signals strong intent to sell.
Also flagged: Probate (inherited property), Recent deed transfer (recently changed hands),
Divorce filing, Eviction filing.
```

Reasons arrive **strongest-first by absolute contribution** (see `ScoringService`), so the
top reason can occasionally be `DEED_TRANSFER` — a *dampener* with a large magnitude. The
template still leads with it; the phrasing reflects the dominant signal, not its sign. If that
reads oddly, the fix belongs in how reasons are ordered/labeled in Phase 5, not here.

## Claude path (opt-in)

`ClaudeLlmProvider` builds its `RestClient` from the injected `RestClient.Builder` and POSTs to
`https://api.anthropic.com/v1/messages`:

| Knob | Value |
|------|-------|
| Model | `claude-haiku-4-5` (cheapest/fastest — ample for two sentences) |
| `max_tokens` | 150 |
| Headers | `x-api-key`, `anthropic-version: 2023-06-01` |
| System prompt | asks for ≤2 plain sentences, no preamble, from the supplied tier/score/reasons |

The reasons/tier/score are rendered into the user turn; the reply text is read from
`content[0].text`. These are documented constants in the class, not external config — only the
API key comes from the environment.

## Testing (no network)

- `MockLlmProviderTest` — non-empty, deterministic, ≤2 sentences, tier framing, single- vs
  multi-reason, and null/empty rejection.
- `ClaudeLlmProviderTest` — a `MockRestServiceServer` bound to the `RestClient.Builder` asserts
  the request (URL, method, headers, model, body) and that the parsed `content[0].text` is
  returned. No real HTTP.
- `LlmProviderWiringTest` — an `ApplicationContextRunner` proves the conditional: no key → the
  sole provider is `MockLlmProvider`; key set → the `@Primary` `ClaudeLlmProvider` is injected.

## v2 levers (intentionally out of scope here)

- **Cache explanations** per `Score` to avoid repeat calls on unchanged leads.
- **Stream / batch** the Claude path if explanations move onto a latency-sensitive surface.
- **Prompt-tune** the system prompt against real lead copy once there's a house voice to match.
