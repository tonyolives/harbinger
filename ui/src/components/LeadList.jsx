import LeadRow from './LeadRow.jsx';

export default function LeadList({ leads }) {
  if (!leads.length) {
    return <p>No leads yet…</p>;
  }
  return (
    <ul style={{ padding: 0 }}>
      {leads.map((lead) => (
        <LeadRow key={lead.homeownerId} lead={lead} />
      ))}
    </ul>
  );
}
