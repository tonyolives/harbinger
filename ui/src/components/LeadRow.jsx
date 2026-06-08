import { tierColor } from '../tierColor.js';

export default function LeadRow({ lead }) {
  const color = tierColor(lead.tier);
  return (
    <li className="lead-card">
      <span className="lead-accent" style={{ backgroundColor: color }} />
      <span
        data-testid="tier-badge"
        className="tier-badge"
        style={{ backgroundColor: color }}
      >
        {lead.tier} {lead.intentScore}
      </span>
      <div className="lead-main">
        <div className="lead-name">{lead.name}</div>
        <div className="lead-address">{lead.address}</div>
        <div className="lead-why">{lead.explanation}</div>
      </div>
    </li>
  );
}
