import { useLeads } from './useLeads.js';
import MetricsBar from './components/MetricsBar.jsx';
import LeadList from './components/LeadList.jsx';

export default function App() {
  const { leads, metrics } = useLeads();
  return (
    <main style={{ maxWidth: 720, margin: '24px auto', fontFamily: 'sans-serif' }}>
      <h1 style={{ fontSize: 20 }}>Harbinger — live leads</h1>
      <MetricsBar metrics={metrics} />
      <LeadList leads={leads} />
    </main>
  );
}
