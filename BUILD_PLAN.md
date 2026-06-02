# Harbinger — Build Plan

A real-time seller-intent pipeline: turn raw public-record signals into ranked, contactable leads, fast. Built as a portfolio piece for a Goliath Data application — **must be simple enough to explain and defend line-by-line.**

**North-star metric:** signal-to-lead latency (time from a seller signal appearing to a ranked lead surfacing). Being first is the whole value.

**Stack:** Spring Boot 3.2 / Java 17 / Maven · JUnit 5 + Cucumber + JaCoCo · React 18 + Vite. Full list and rationale in `TECH_STACK.md`. Conventions in `AGENTS.md`.

---

## Architecture (one pipeline, mirrors ARGUS's filter pipeline)

```
synthetic signals → normalize → resolve (dedupe to one homeowner) → enrich + mock skiptrace → score (hot/warm/cold) → [hot?] → alert + surface lead → SSE → map UI
```

Packages: `com.harbinger.{controller, service, service.pipeline, service.ingest, repository, model, dto, config, llm, bench}`. Thin controllers → services → DTOs. In-memory store (no database).

---

## Phases (do in order; each: branch → tests first → implement → green → PR to `develop`)

**Phase 1 — Models + synthetic data** · `feature/models-datagen`
- Tests: models reject bad input; generator is deterministic per seed; one owner produces several *messy* name/address variants sharing one `trueOwnerId`.
- Build: `RawSignal`, `Homeowner`, `Lead`, enums (`Source`, `SignalType`, `Tier`); `SignalGeneratorService` (datafaker + perturbations).
- Done: prints a deterministic stream of labeled messy signals.

**Phase 2 — Normalize** · `feature/ingest`
- Tests: messy names/addresses collapse to canonical keys; normalizing twice == once.
- Build: `NameNormalizer`, `AddressNormalizer`.

**Phase 3 — Entity resolution (the centerpiece)** · `feature/resolution`
- Tests: clean set → precision/recall = 1.0; noisy set → precision ≥ 0.95, recall ≥ 0.85 and beats naive exact-match; same person/diff spelling → merged; different people/same address → NOT merged.
- Build: `ResolutionService` — block by address key, fuzzy-match with commons-text (JaroWinkler), cluster with union-find. `evaluate()` computes P/R/F1 (the ONLY place `trueOwnerId` is read).

**Phase 4 — Enrich + mock skip-trace** · `feature/enrich`
- Tests: deterministic property fields; equity never negative-by-bug; contact is obviously fake and flagged `mock: true`.
- Build: `EnrichmentService`.

**Phase 5 — Scoring** · `feature/scoring`
- Tests (pure, inject `Clock`): adding a signal never lowers score; older signal contributes less (recency decay); stacked signals score higher; tier thresholds correct; `DEED_TRANSFER` dampens.
- Build: `ScoringService` — weighted signals + recency decay → score (0–100), tier, reasons. Weights centralized; documented in `docs/SCORING.md`.

**Phase 6 — Explanations** · `feature/explain`
- Tests (Mockito, no network): `MockLlmProvider` returns a ≤2-sentence "why this lead" from the reasons; real provider only used when `ANTHROPIC_API_KEY` set.
- Build: `LlmProvider` interface + `MockLlmProvider` (default) + `ClaudeLlmProvider` (opt-in, RestClient).

**Phase 7 — Real-time loop + API** · `feature/realtime-api`
- Tests: a homeowner crossing HOT fires exactly one alert and a lead with `signalToLeadMs` measured (inject `Clock`); leads stay ranked; `GET /api/v1/leads`, `/leads/{id}`, `/metrics`, and SSE `/stream` behave (MockMvc).
- Build: `SignalPipeline` + orchestrator driving the stream; `LeadController` + `SseEmitter`; DTOs; `@ControllerAdvice`; CORS for `:5173`.

**Phase 8 — Benchmark, UI, ship** · `feature/bench-ui-docs`
- Bench: fixed-seed run writes `benchmarks/report.json` + a chart PNG (XChart): resolution F1, tier counts, signal-to-lead p50/p95, throughput.
- UI (Jest + RTL first): React + Vite + MapLibre/deck.gl; leads colored by tier, ranked side panel, live updates via `EventSource`.
- Docs: README with real numbers + 45-sec Loom; `docs/SCORING.md`. Merge `develop` → `main`, tag `v0.1.0-demo`.

---

## Schedule
Mon: P1 (+start P2) · Tue: P3 · Wed: P4–P5 · Thu: P6–P7 (be demo-able tonight) · Fri: P8 + ship.

## Guardrails (non-negotiable)
- All data **synthetic**; no real scraping, skip-tracing, or PII. Mock contact is obviously fake.
- Don't overclaim: "models the pipeline on synthetic data," not "predicts who will sell." Scoring is rules-based v1.
- One README line on compliance awareness (FCRA/DNC/state record laws) for the real version.

## Gitflow
`main` / `develop` / one `feature/*` per phase → PR into `develop`. Conventional Commits. CI green before merge.
