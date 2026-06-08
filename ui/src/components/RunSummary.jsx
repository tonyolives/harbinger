import { summarize } from '../leadUtils.js';
import { TIER_COLOR } from '../tierColor.js';

/**
 * Post-run recap for the demo: hidden until the feed finishes, then surfaces at the bottom of the
 * page with the story numbers — messy signals resolved into a handful of unique, actionable,
 * low-latency leads. Stat tiles reuse the same `.tile` styling as the live MetricsBar; the tier bar
 * visualizes the HOT/WARM/COLD split using the shared tier colors.
 */
export default function RunSummary({ done, metrics, leads = [], elapsedMs = 0 }) {
  if (!done || !metrics) {
    return null;
  }

  const s = summarize(metrics, leads, elapsedMs);

  return (
    <section className="panel" data-testid="run-summary">
      <div className="panel-title">Run summary</div>

      <p className="summary-headline">
        Resolved <strong>{s.signalsProcessed}</strong> messy signals into{' '}
        <strong>{s.leadsSurfaced}</strong> unique homeowner leads in{' '}
        <strong>{s.durationSec.toFixed(1)}s</strong> — {s.hotCount} hot, p95 {s.p95Ms}ms.
      </p>

      <div className="metrics summary-tiles">
        <Tile testid="summary-consolidation" value={`${s.consolidation.toFixed(1)}×`} label="Signal consolidation" />
        <Tile testid="summary-leads" value={`${s.signalsProcessed} → ${s.leadsSurfaced}`} label="Signals → owners" />
        <Tile testid="summary-hot-rate" value={`${s.hotRatePct}%`} label="Hot lead rate" />
        <Tile testid="summary-actionable" value={`${s.actionable} (${s.actionablePct}%)`} label="Actionable (hot+warm)" />
        <Tile testid="summary-avg-score" value={s.avgScore} label="Avg intent score" />
        <Tile testid="summary-duration" value={`${s.durationSec.toFixed(1)}s`} label="Run duration" />
        <Tile testid="summary-throughput" value={`${s.throughput.toFixed(1)}/s`} label="Throughput" />
        <Tile testid="summary-latency" value={`${s.p50Ms} / ${s.p95Ms}ms`} label="Latency p50 / p95" />
      </div>

      <TierBar tiers={s.tiers} />
    </section>
  );
}

function Tile({ value, label, testid }) {
  return (
    <div className="tile" data-testid={testid}>
      <div className="tile-value">{value}</div>
      <div className="tile-label">{label}</div>
    </div>
  );
}

/** Horizontal stacked bar of the tier split, with a labeled legend below. */
function TierBar({ tiers }) {
  const segments = [
    { key: 'HOT', ...tiers.hot },
    { key: 'WARM', ...tiers.warm },
    { key: 'COLD', ...tiers.cold },
  ];
  return (
    <div className="tier-bar-wrap" data-testid="tier-bar">
      <div className="tier-bar">
        {segments.map((seg) =>
          seg.pct > 0 ? (
            <div
              key={seg.key}
              className="tier-bar-seg"
              style={{ width: `${seg.pct}%`, backgroundColor: TIER_COLOR[seg.key] }}
              title={`${seg.key} ${seg.count} (${seg.pct}%)`}
            />
          ) : null,
        )}
      </div>
      <div className="tier-bar-legend">
        {segments.map((seg) => (
          <span key={seg.key} className="tier-bar-legend-item">
            <span className="dot" style={{ backgroundColor: TIER_COLOR[seg.key] }} />
            {seg.key.toLowerCase()} {seg.count} · {seg.pct}%
          </span>
        ))}
      </div>
    </div>
  );
}
