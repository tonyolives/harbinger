# Harbinger — Tech Stack

Simple reference for me and any AI agent working on this repo. This is the allowed dependency list. Do not add anything outside it without a one-line reason.

**Principle:** keep it simple. This is a portfolio demo that I need to explain line by line. Familiar beats fancy.

## Project Setup

- **Java 17**
- **Spring Boot 3.2.12** on the Spring Boot 3.2 line
- **Maven** via the wrapper (`./mvnw`), pinned to Maven 3.9.14
- Packaging: **Jar**
- Config: **YAML** (`application.yaml`)
- Base package: **`com.harbinger`**

## Backend

| Tech | Status | Why |
|---|---|---|
| Spring Boot 3.2.12 | In `pom.xml` | Familiar stack, strong testing support, simple DI, easy to explain |
| Spring Web | In `pom.xml` | REST endpoints, SSE stream, and `RestClient` for the opt-in LLM provider |
| Spring Validation | In `pom.xml` | Reject bad input at boundaries |
| Spring Boot Actuator | In `pom.xml` | Health and simple metrics endpoints |
| Apache Commons Text | In `pom.xml` | Fuzzy matching with Jaro-Winkler for entity resolution |
| DataFaker | In `pom.xml` | Synthetic, PII-free signal generation |
| XChart | In `pom.xml` | Benchmark chart PNG generation |

## Storage

| Tech | Status | Why |
|---|---|---|
| In-memory repositories | Done (Phase 7) | No database needed for the demo; deterministic and easy to replace later (`LeadRepository`) |
| `ConcurrentHashMap` behind interfaces | Done (Phase 7) | Simple thread-safe storage for the in-process pipeline (`InMemoryLeadRepository`) |

## LLM

| Tech | Status | Why |
|---|---|---|
| `LlmProvider` interface | Done (Phase 6) | Keeps explanation generation decoupled from the pipeline |
| `MockLlmProvider` | Done (Phase 6 default) | Deterministic templates; no API key and no test network |
| `ClaudeLlmProvider` | Done (Phase 6 opt-in) | Uses Spring `RestClient` only when `ANTHROPIC_API_KEY` is set |

## Testing

| Tech | Status | Why |
|---|---|---|
| JUnit 5 | In `pom.xml` via `spring-boot-starter-test` | Unit and Spring tests |
| Mockito | In `pom.xml` via `spring-boot-starter-test` | Mock interfaces such as `LlmProvider` and `RestClient` |
| AssertJ | In `pom.xml` via `spring-boot-starter-test` | Readable assertions |
| JaCoCo | In `pom.xml` | Coverage reports and the 90% line coverage gate |

## Frontend

In `ui/` (plain-JS, intentionally minimal for v1).

| Tech | Status | Why |
|---|---|---|
| React 18 + Vite | In `ui/` (Phase 8) | Fast local UI with a small component model |
| EventSource | In `ui/` (Phase 8) | Browser-native SSE for live lead updates |
| Jest + React Testing Library | In `ui/` (Phase 8) | Frontend tests for UI behavior |

## Tooling

| Tech | Status | Why |
|---|---|---|
| Gitflow | In use | `main`, `develop`, and one `feature/*` branch per phase |
| Conventional Commits | In use | Clean, readable history |
| Maven wrapper | In use | Reproducible Maven command without relying on a global install |
| `exec-maven-plugin` | In `pom.xml` (Phase 8) | Runs the benchmark `main` headless (`make bench`) without booting the web app |
| Makefile | In use (Phase 8) | Shortcuts: `make bench`, `make test`, `make run` |
| GitHub Actions | In `.github/workflows/ci.yml` | Runs `./mvnw verify` and the UI Jest suite on pushes and PRs to `main`/`develop` |

## Not Using

- **Spring Data JPA / Postgres / PostGIS** — in-memory only for v0.
- **Docker / TestContainers** — out of scope for the demo.
- **Kafka / RabbitMQ / WebSockets** — SSE plus an in-process stream is enough.
- **Real scraping, skip tracing, or real PII** — synthetic data only.
- **Trained ML model** — no real outcome labels; scoring is rules-based v1.
- **Auth / Spring Security** — no users or accounts in the local demo.
- **springdoc-openapi** — not currently in `pom.xml`; add only with a one-line reason.
- **Lombok** — plain Java keeps the interview explanation clearer.

## Version Pins

| Dependency | Version |
|---|---:|
| Spring Boot | 3.2.12 |
| Java | 17 |
| Maven wrapper | 3.9.14 |
| Commons Text | 1.15.0 |
| DataFaker | 2.5.4 |
| JaCoCo | 0.8.14 |
| XChart | 3.8.8 |
