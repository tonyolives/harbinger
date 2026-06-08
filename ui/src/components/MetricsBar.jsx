import { TIER_COLOR } from '../tierColor.js';

/** Stat tiles for the pipeline's live numbers, plus the tier breakdown as colored chips. */
export default function MetricsBar({ metrics }) {
  if (!metrics) {
    return null;
  }
  return (
    <>
      <div className="metrics">
        <Tile testid="metric-signals" value={metrics.signalsProcessed} label="Signals" />
        <Tile testid="metric-leads" value={metrics.leadsSurfaced} label="Leads" />
        <Tile testid="metric-p50" value={`${metrics.signalToLeadP50Ms}ms`} label="p50 latency" />
        <Tile testid="metric-p95" value={`${metrics.signalToLeadP95Ms}ms`} label="p95 latency" />
      </div>
      <div className="tier-chips">
        <Chip tier="HOT" count={metrics.hotCount} />
        <Chip tier="WARM" count={metrics.warmCount} />
        <Chip tier="COLD" count={metrics.coldCount} />
      </div>
    </>
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

function Chip({ tier, count }) {
  return (
    <span className="chip">
      <span className="dot" style={{ backgroundColor: TIER_COLOR[tier] }} />
      {count} {tier.toLowerCase()}
    </span>
  );
}
