import { useCallback, useEffect, useRef, useState } from 'react';
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
 *
 * The feed has no explicit "done" signal, so completion is inferred on the client: the generator
 * emits exactly `owners × signalsPerOwner` signals, so the run is finished once
 * `metrics.signalsProcessed` reaches that count. An idle-timeout fallback (no events for a few feed
 * intervals) also marks it done, so the summary always eventually appears. On completion `running`
 * flips off, `done` flips on, and `elapsedMs` captures the wall-clock duration.
 */
export function useLeads() {
  const [leads, setLeads] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [running, setRunning] = useState(false);
  const [done, setDone] = useState(false);
  const [elapsedMs, setElapsedMs] = useState(0);
  const [expectedSignals, setExpectedSignals] = useState(0);

  // Refs the completion logic reads without re-subscribing the SSE effect or re-arming timers.
  const startedAtRef = useRef(0);
  const feedDelayRef = useRef(0);
  const idleTimerRef = useRef(null);

  const finish = useCallback(() => {
    if (idleTimerRef.current) {
      clearTimeout(idleTimerRef.current);
      idleTimerRef.current = null;
    }
    setRunning(false);
    setDone(true);
    setElapsedMs(startedAtRef.current ? Date.now() - startedAtRef.current : 0);
  }, []);

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

  // Completion detection. Primary path: signal count reached the expected total. Fallback: no new
  // metrics for several feed intervals (sized off the chosen feed speed). Whichever fires first
  // ends the run exactly once (guarded by `running`).
  useEffect(() => {
    if (!running || !metrics) {
      return undefined;
    }
    if (expectedSignals > 0 && metrics.signalsProcessed >= expectedSignals) {
      finish();
      return undefined;
    }
    const idleMs = Math.max(1500, feedDelayRef.current * 4);
    idleTimerRef.current = setTimeout(finish, idleMs);
    return () => {
      if (idleTimerRef.current) {
        clearTimeout(idleTimerRef.current);
        idleTimerRef.current = null;
      }
    };
  }, [metrics, running, expectedSignals, finish]);

  const start = useCallback((params) => {
    setLeads([]);
    setMetrics(EMPTY_METRICS);
    setDone(false);
    setElapsedMs(0);
    setExpectedSignals((params.owners || 0) * (params.signalsPerOwner || 0));
    startedAtRef.current = Date.now();
    feedDelayRef.current = params.feedDelayMs || 0;
    setRunning(true);
    return fetch('/api/v1/demo/start', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(params),
    }).catch(() => {});
  }, []);

  const reset = useCallback(() => {
    if (idleTimerRef.current) {
      clearTimeout(idleTimerRef.current);
      idleTimerRef.current = null;
    }
    setRunning(false);
    setDone(false);
    setElapsedMs(0);
    setExpectedSignals(0);
    setLeads([]);
    setMetrics(EMPTY_METRICS);
    return fetch('/api/v1/demo/reset', { method: 'POST' }).catch(() => {});
  }, []);

  return { leads, metrics, running, done, elapsedMs, start, reset };
}
