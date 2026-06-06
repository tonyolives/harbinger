# Scoring model (Phase 5)

Turns a resolved homeowner's raw signals into an intent **score (0–100)**, a **tier**
(COLD / WARM / HOT), and a short list of human-readable **reasons**. The reasons later feed
`Lead.reasons()` and the Phase 6 LLM explanation.

This is a **rules-based v1**: there are no real seller outcomes to train on, so the weights
below are an explainable prior, not a learned model. The whole computation is a pure
function of the signals and the current time (`java.time.Clock` is injected), which keeps it
deterministic and unit-testable.

## Formula

```
ageDays = max(0, days between signal.observedAt and now)   // future-dated signals → age 0
decay   = 0.5 ^ (ageDays / 60.0)                           // 60-day half-life
raw     = Σ  weight(signalType) · decay                    // summed over every signal
score   = round( clamp(0, 100, raw) )                      // floor at 0, cap at 100
```

Tier is derived from the final score:

| Tier | Score      |
|------|------------|
| HOT  | ≥ 70       |
| WARM | 40 – 69    |
| COLD | < 40       |

## Weights

Weights live in one place — an `EnumMap<SignalType, Double>` constant in `ScoringService` —
so the model is auditable. `DEED_TRANSFER` is a **negative** dampener: a home that just
changed hands is *less* likely to be on the market, so it pulls the score down.

| SignalType        | Weight | Rationale |
|-------------------|-------:|-----------|
| PRE_FORECLOSURE   |   45   | Strongest distress / time pressure |
| PROBATE           |   35   | Inherited property, frequently sold |
| TAX_DELINQUENCY   |   30   | Financial strain |
| DIVORCE           |   25   | Forced asset split |
| EVICTION          |   20   | Landlord distress |
| DEED_TRANSFER     |  −30   | Dampener — home just changed hands |

## Recency decay

A signal's contribution halves every **60 days**. So a 2-month-old pre-foreclosure counts
~22.5 instead of 45, and a 4-month-old one counts ~11.25. Being first is the whole value of
the pipeline, so fresher signals dominate — but older context still nudges the score rather
than vanishing. Future-dated signals (clock skew in tests or data) are clamped to age 0 so
decay never exceeds 1.

## Properties (and why they hold)

- **Adding a positive signal never lowers the score.** Every positive weight adds a
  non-negative decayed amount to `raw`, and `clamp` is non-decreasing. This monotonicity is
  scoped to *positive* signals — `DEED_TRANSFER` is the deliberate exception below.
- **Older contributes less.** `decay` shrinks with age, so the same signal dated earlier
  adds less.
- **Stacking scores higher.** Two sub-cap signals sum to more than either alone.
- **`DEED_TRANSFER` dampens.** Its negative weight lowers `raw`, so a set containing it
  scores below the same set without it. A lone `DEED_TRANSFER` floors at 0 / COLD.
- **Always in range.** The floor at 0 and cap at 100 keep the score honest no matter how many
  signals stack.

## v2 levers (intentionally out of scope here)

Phase 5 scores from **signals + recency only**. Richer inputs are deferred so each phase stays
explainable:

- **Equity % / LTV and market value** — enrichment already exposes these
  (`PropertyDetails.equity`, `estimatedMarketValue`); a high-equity distressed owner is a
  stronger lead. Deliberately excluded from v1 to keep scoring and enrichment decoupled.
- **Per-source confidence** — weight a signal by how reliable its `Source` is.
- **Exponential-saturating curve** — replace the linear cap with `100·(1 − e^(−raw/K))` for a
  smoother approach to 100.
- **Learned weights** — once real outcome labels exist, fit the weights instead of hand-setting them.
