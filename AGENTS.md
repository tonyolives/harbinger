# AGENTS.md — Harbinger

Conventions for any AI coding agent working on this repo. Read this and `BUILD_PLAN.md` before writing code.

## What this is
A real-time seller-intent pipeline (synthetic data) built as a portfolio piece for a job application. **The human author must be able to explain and defend every line in an interview.** Therefore: prefer simple, clear, conventional code over clever abstractions. When in doubt, choose the more obvious solution.

## Golden rules
1. **Test-first.** Write failing tests (JUnit for the backend, Jest for the UI) before implementation. Show the human the tests before implementing if asked.
2. **One phase at a time** from `BUILD_PLAN.md`. Don't start a phase until the previous one is green.
3. **Run `./mvnw verify` after every change.** JaCoCo line coverage ≥ 90% on touched packages.
4. **`ScoringService` and `ResolutionService` core logic must be pure** — no I/O, no `Instant.now()`; inject `java.time.Clock`.
5. **No network in tests.** Mock `LlmProvider` / `RestClient` with Mockito. `MockLlmProvider` is the default and needs no API key.
6. **Never reference `trueOwnerId`** in `service`, `controller`, or `repository` code — it's a ground-truth label for tests and the benchmark only. Reading it elsewhere makes the resolution metric meaningless.
7. **Thin controllers.** Logic lives in services; DTOs are decoupled from domain models.
8. **Don't add dependencies** outside `TECH_STACK.md` without a one-line justification.
9. **Don't build out-of-scope items** (see below). If tempted, stop and ask.
10. **Conventional Commits** (`feat:`, `test:`, `fix:`, `refactor:`, `docs:`). One feature branch per phase off `develop`.
11. **The human owner handles git commits, pushes, and PR creation** unless explicitly asking the agent to do so.

## Stack
Spring Boot 3.2, Java 17, Maven. React 18 + Vite frontend. Full list and rationale: `TECH_STACK.md`. Don't reach outside it.

## Out of scope (do NOT build)
Real scraping or data sources · real skip-tracing or any real PII · auth/accounts · a trained ML scoring model (rules-based v1 only) · Postgres/PostGIS/Docker/TestContainers (in-memory only) · Kafka/RabbitMQ/WebSockets (use SSE).

## Commands
- Backend tests + coverage: `./mvnw verify`
- Run backend: `./mvnw spring-boot:run`
- Frontend: `cd ui && npm install && npm run dev`
- Frontend tests: `cd ui && npm test`
- Benchmark: `make bench` (writes `benchmarks/report.json` + chart)

## Definition of done (per phase)
Tests pass · coverage ≥ 90% on touched packages · committed on the phase's feature branch · PR opened into `develop` · no out-of-scope additions · README metrics (if touched) come only from generated `benchmarks/report.json`.
