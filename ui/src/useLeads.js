import { useEffect, useState } from 'react';
import { rank } from './leadUtils.js';

/**
 * Loads the ranked leads + metrics, then refreshes them on every SSE `lead` event. The event is
 * used as a change notification and the authoritative state is re-read from `/api/v1/leads` — that
 * way the UI always matches the backend exactly, including when a homeowner's id consolidates and
 * a tentative row is pruned. Closes the stream on unmount; network failures are swallowed.
 */
export function useLeads() {
  const [leads, setLeads] = useState([]);
  const [metrics, setMetrics] = useState(null);

  useEffect(() => {
    let active = true;

    const loadLeads = () =>
      fetch('/api/v1/leads')
        .then((r) => r.json())
        .then((data) => active && setLeads(rank(data)))
        .catch(() => {});
    const loadMetrics = () =>
      fetch('/api/v1/metrics')
        .then((r) => r.json())
        .then((m) => active && setMetrics(m))
        .catch(() => {});

    loadLeads();
    loadMetrics();

    const source = new EventSource('/api/v1/stream');
    source.addEventListener('lead', () => {
      loadLeads();
      loadMetrics();
    });

    return () => {
      active = false;
      source.close();
    };
  }, []);

  return { leads, metrics };
}
