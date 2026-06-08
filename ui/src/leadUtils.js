// Pure helper for the lead list. The backend already returns ranked leads; this re-sorts
// defensively so the UI never depends on response ordering.

/** Strongest first (highest intent score). Returns a new array. */
export function rank(leads) {
  return [...leads].sort((a, b) => b.intentScore - a.intentScore);
}

/** Percentage of `part` out of `whole`, rounded to a whole number; 0 when `whole` is 0. */
function pct(part, whole) {
  return whole > 0 ? Math.round((part / whole) * 100) : 0;
}

/**
 * Derives the post-run demo summary from the final metrics snapshot, the surfaced leads, and the
 * run's wall-clock duration. Pure (mirrors {@link rank}) so it's unit-testable in isolation. Every
 * ratio guards divide-by-zero so an empty or instantaneous run yields zeros rather than NaN.
 *
 * @param metrics   the `/api/v1/metrics` snapshot (signals/leads/tier counts + latency percentiles)
 * @param leads     surfaced leads (used only for the average intent score)
 * @param elapsedMs wall-clock ms from pressing Begin to the run completing
 */
export function summarize(metrics, leads = [], elapsedMs = 0) {
  const {
    signalsProcessed = 0,
    leadsSurfaced = 0,
    hotCount = 0,
    warmCount = 0,
    coldCount = 0,
    signalToLeadP50Ms = 0,
    signalToLeadP95Ms = 0,
  } = metrics ?? {};

  const actionable = hotCount + warmCount;
  const durationSec = elapsedMs / 1000;
  const avgScore =
    leads.length > 0
      ? Math.round(leads.reduce((sum, l) => sum + (l.intentScore ?? 0), 0) / leads.length)
      : 0;

  return {
    // Entity resolution
    signalsProcessed,
    leadsSurfaced,
    consolidation: leadsSurfaced > 0 ? signalsProcessed / leadsSurfaced : 0,
    // Lead quality
    hotCount,
    hotRatePct: pct(hotCount, leadsSurfaced),
    actionable,
    actionablePct: pct(actionable, leadsSurfaced),
    avgScore,
    // Speed & throughput
    durationSec,
    throughput: durationSec > 0 ? signalsProcessed / durationSec : 0,
    // Latency (north-star)
    p50Ms: signalToLeadP50Ms,
    p95Ms: signalToLeadP95Ms,
    // Tier split (for the distribution bar)
    tiers: {
      hot: { count: hotCount, pct: pct(hotCount, leadsSurfaced) },
      warm: { count: warmCount, pct: pct(warmCount, leadsSurfaced) },
      cold: { count: coldCount, pct: pct(coldCount, leadsSurfaced) },
    },
  };
}
