export default function MetricsBar({ metrics }) {
  if (!metrics) {
    return null;
  }
  return (
    <div style={{ fontFamily: 'monospace', marginBottom: 16, color: '#333' }}>
      <div>
        signals {metrics.signalsProcessed} · leads {metrics.leadsSurfaced} · p50{' '}
        {metrics.signalToLeadP50Ms}ms · p95 {metrics.signalToLeadP95Ms}ms
      </div>
      <div>
        {metrics.hotCount} hot · {metrics.warmCount} warm · {metrics.coldCount} cold
      </div>
    </div>
  );
}
