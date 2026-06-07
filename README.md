# harbinger
A real-time seller-intent pipeline that turns raw public-record signals into ranked, contactable leads

## Run

```sh
./mvnw spring-boot:run     # boots the API and replays a synthetic signal stream
./mvnw verify              # tests + coverage
```

Optional: set `ANTHROPIC_API_KEY` to generate lead explanations with Claude instead of the default deterministic template.

## API (`/api/v1`)

| Endpoint | What it returns |
|----------|-----------------|
| `GET /api/v1/leads` | Surfaced leads, ranked strongest-first |
| `GET /api/v1/leads/{id}` | One lead by homeowner id (404 if none) |
| `GET /api/v1/metrics` | Signals processed, leads surfaced, tier counts, signal-to-lead p50/p95 |
| `GET /api/v1/stream` | Server-Sent Events — a `lead` event each time a lead surfaces |

```sh
curl -N localhost:8080/api/v1/stream      # watch leads surface live
curl localhost:8080/api/v1/leads
```

> Synthetic data only — no real people, scraping, or contact lookups. Scoring is a transparent rules-based v1. See `PRODUCT_OVERVIEW.md`.
