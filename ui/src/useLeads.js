import { useCallback, useEffect, useState } from 'react';
import { rank } from './leadUtils.js';

const EMPTY_METRICS = {
  signalsProcessed: 0,
  leadsSurfaced: 0,
  hotCount: 0,
  warmCount: 0,
  coldCount: 0,
  signalToLeadP50Ms: 0,
  signalToLeadP95Ms: 0,
};

/**
 * Loads the ranked leads + metrics, then refreshes them on every SSE `lead` event. The event is
 * used as a change notification and the authoritative state is re-read from `/api/v1/leads` — that
 * way the UI always matches the backend exactly, including when a homeowner's id consolidates and
 * a tentative row is pruned. Closes the stream on unmount; network failures are swallowed.
 *
 * Also exposes `start(params)` / `reset()` (driving the demo from the control panel) and a
 * `running` flag. Begin/Reset optimistically clear the local list so it visibly empties, then the
 * incoming SSE events repopulate it.
 */
export function useLeads() {
  const [leads, setLeads] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [running, setRunning] = useState(false);

  const loadLeads = useCallback(
    () =>
      fetch('/api/v1/leads')
        .then((r) => r.json())
        .then((data) => setLeads(rank(data)))
        .catch(() => {}),
    [],
  );
  const loadMetrics = useCallback(
    () =>
      fetch('/api/v1/metrics')
        .then((r) => r.json())
        .then((m) => setMetrics(m))
        .catch(() => {}),
    [],
  );

  useEffect(() => {
    loadLeads();
    loadMetrics();

    const source = new EventSource('/api/v1/stream');
    source.addEventListener('lead', () => {
      loadLeads();
      loadMetrics();
    });

    return () => source.close();
  }, [loadLeads, loadMetrics]);

  const start = useCallback(
    (params) => {
      setLeads([]);
      setMetrics(EMPTY_METRICS);
      setRunning(true);
      return fetch('/api/v1/demo/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(params),
      }).catch(() => {});
    },
    [],
  );

  const reset = useCallback(() => {
    setRunning(false);
    setLeads([]);
    setMetrics(EMPTY_METRICS);
    return fetch('/api/v1/demo/reset', { method: 'POST' }).catch(() => {});
  }, []);

  return { leads, metrics, running, start, reset };
}
