# harbinger
A real-time seller-intent pipeline that turns raw public-record signals into ranked, contactable leads — on synthetic data, built to be explainable line by line.

`generate → normalize → resolve → enrich → score → explain → real-time loop + API → UI`

## Run

Backend (API + a paced replay of a synthetic signal stream):

```sh
./mvnw spring-boot:run     # http://localhost:8080
./mvnw verify              # tests + coverage
```

UI (live lead list, Vite dev server — proxies `/api` to the backend):

```sh
cd ui && npm install && npm run dev   # http://localhost:5173
cd ui && npm test                     # Jest + React Testing Library
```

Run both (each in its own terminal), then open http://localhost:5173 to watch leads surface and re-rank live.

Optional: set `ANTHROPIC_API_KEY` to generate lead explanations with Claude (`claude-haiku-4-5`) instead of the default deterministic template.

## API (`/api/v1`)

| Endpoint | What it returns |
|----------|-----------------|
| `GET /api/v1/leads` | Leads, ranked strongest-first |
| `GET /api/v1/leads/{id}` | One lead by homeowner id (404 if none) |
| `GET /api/v1/metrics` | Signals processed, leads, tier counts, signal-to-lead p50/p95 |
| `GET /api/v1/stream` | Server-Sent Events — a `lead` event whenever a lead is created or changes |

```sh
curl -N localhost:8080/api/v1/stream      # watch leads update live
curl localhost:8080/api/v1/leads
```

## Benchmark

`make bench` runs the pipeline on a fixed seed and writes `benchmarks/report.json` + a tier chart. Numbers below are from that report (seed 2, 8 homeowners, 48 messy signals); regenerate any time with `make bench`.

| Metric | Value |
|---|---|
| Entity resolution (clean set) | precision **1.000** · recall **1.000** · F1 **1.000** |
| Homeowners resolved | 8 from 48 signals |
| Tier split | 6 hot · 2 warm · 0 cold |
| Leads surfaced | 8 |
| Signal-to-lead latency | p50 **< 1 ms**, p95 **~6 ms** (in-process) |
| Throughput | ~2,700 signals/s |

Signal-to-lead latency is the north-star metric — time from a signal arriving to a ranked lead surfacing. It's small because the demo pipeline runs in-process on synthetic data; the point is the architecture, measured honestly, not a production SLA.

## Honest scope

- **Synthetic data only** — no real people, scraping, or contact lookups; mock contact is obviously fake.
- **Rules-based scoring v1** — a transparent, explainable prior, not a trained model. See `docs/SCORING.md`.
- **Not production** — a real version needs compliance work (FCRA-adjacent data, DNC, state record-access laws).

More: `PRODUCT_OVERVIEW.md` (what & why), `docs/SCORING.md` (scoring), `docs/EXPLANATIONS.md` (LLM explanations), `BUILD_PLAN.md` (phases), `TECH_STACK.md` (stack).
