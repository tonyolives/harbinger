import { tierColor } from '../tierColor.js';

export default function LeadRow({ lead }) {
  return (
    <li style={{ marginBottom: 10, listStyle: 'none' }}>
      <span
        data-testid="tier-badge"
        style={{
          backgroundColor: tierColor(lead.tier),
          color: 'white',
          padding: '2px 6px',
          borderRadius: 3,
          marginRight: 8,
          fontFamily: 'monospace',
        }}
      >
        {lead.tier} {lead.intentScore}
      </span>
      <strong>{lead.name}</strong> — {lead.address}
      <div style={{ color: '#555', fontSize: 13 }}>{lead.explanation}</div>
    </li>
  );
}
