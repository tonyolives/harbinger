# Harbinger — Product Overview

## What it is

Harbinger is a real-time seller-intent pipeline. It watches a stream of public-record signals, figures out which homeowner each signal belongs to, scores how likely that homeowner is to sell, and surfaces the hottest leads the moment they appear — measuring how fast it can do it.

It's a working, honest miniature of the kind of data pipeline **Goliath Data** runs, built on synthetic data to prove I understand and can implement their core engineering problem.

## The problem it models

Real-estate agents and investors win by reaching a motivated homeowner **before anyone else**. Signals that someone might sell — a pre-foreclosure notice, tax delinquency, probate filing, divorce, eviction — are scattered across messy public records (county recorder, courts, assessors). The hard part is turning that noisy firehose into _the right homeowner, ranked by intent, in your hands first_. Speed is the product.

## How it works (plain English)

1. **Ingest** — a stream of raw signals arrives, each with a messy name and address.
2. **Normalize** — names and addresses are cleaned into comparable form.
3. **Resolve** — fuzzy matching merges the messy records into a single homeowner identity (this is the hard part; it's measured for accuracy).
4. **Enrich** — each homeowner gets property details and a (mock) phone/email.
5. **Score** — signals combine into a 0–100 intent score and a hot/warm/cold tier, with plain-language reasons.
6. **Alert** — the instant a homeowner turns hot, a ranked lead is surfaced and the time-to-surface is recorded.
7. **View** — leads appear live on a map, colored by tier.

## What it is NOT (deliberately honest)

- **Not real data.** Everything is synthetic — no real people, no scraping, no real contact lookups.
- **Not a trained model.** Scoring is a transparent, rules-based v1. With real "did they sell" outcome data, I'd train a calibrated model ; I don't claim predictive accuracy on synthetic data.
- **Not production.** A real version would need compliance work (FCRA-adjacent data, DNC rules, state record-access laws).

## Why it's the same pattern as my other project (ARGUS)

ARGUS ingests live global event feeds and surfaces what matters in real time through a multi-stage filter pipeline. Harbinger is that same architecture — ingest → process → score/filter → surface — pointed at property signals instead of geopolitical events. Same backbone, same testing discipline, different domain.

## Key metric

**Signal-to-lead latency** — measured end to end, because being first is the entire value.

## Tech at a glance

Spring Boot 3.2 / Java 17 backend, real-time pipeline with SSE, React + map UI, fully test-driven (JUnit + Cucumber). Full rationale in `TECH_STACK.md`.

## How I'd extend it

Train the scoring model on real sale outcomes; add real data-source connectors behind the existing pipeline interfaces; persist to Postgres/PostGIS for spatial queries; harden for compliance. The architecture is built so each of these drops in without a rewrite.
