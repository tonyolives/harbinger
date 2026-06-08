import { useLeads } from './useLeads.js';
import ControlPanel from './components/ControlPanel.jsx';
import MetricsBar from './components/MetricsBar.jsx';
import LeadList from './components/LeadList.jsx';

export default function App() {
  const { leads, metrics, running, start, reset } = useLeads();
  const throughput = metrics?.signalsProcessed ? `${metrics.signalsProcessed} signals processed` : '—';

  return (
    <div className="app-shell">
      <header className="app-header">
        <div className="brand">
          <span className="brand-mark">◢</span>
          <div>
            <h1>Harbinger</h1>
            <p className="subtitle">Real-time seller-intent leads</p>
          </div>
        </div>
        <div className="header-right">
          <span className={`live${running ? '' : ' idle'}`}>
            <span className="live-dot" />
            {running ? 'LIVE' : 'IDLE'}
          </span>
          <div className="throughput">{throughput}</div>
        </div>
      </header>

      <ControlPanel running={running} onStart={start} onReset={reset} />
      <MetricsBar metrics={metrics} />
      <LeadList leads={leads} />
    </div>
  );
}
