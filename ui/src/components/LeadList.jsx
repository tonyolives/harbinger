import LeadRow from './LeadRow.jsx';

export default function LeadList({ leads }) {
  if (!leads.length) {
    return <div className="empty-state">Waiting for signals — press Begin to start the feed.</div>;
  }
  return (
    <ul className="lead-list">
      {leads.map((lead) => (
        <LeadRow key={lead.homeownerId} lead={lead} />
      ))}
    </ul>
  );
}
