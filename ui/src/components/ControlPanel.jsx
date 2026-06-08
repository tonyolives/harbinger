import { useState } from 'react';

const DEFAULTS = {
  seed: 2,
  owners: 8,
  signalsPerOwner: 6,
  feedDelayMs: 300,
  hardMode: false,
};

/**
 * The demo's cockpit: tune the signal parameters, then Begin the live feed (or Reset to empty).
 * Begin posts the current field values to `/api/v1/demo/start`; the resulting leads stream back in
 * over SSE and surface in the list. Values are also clamped server-side, so these inputs are just
 * the friendly front door.
 */
export default function ControlPanel({ running, onStart, onReset }) {
  const [form, setForm] = useState(DEFAULTS);

  const setNumber = (key) => (e) =>
    setForm((f) => ({ ...f, [key]: Number(e.target.value) }));

  const begin = () => onStart(form);

  return (
    <section className="panel">
      <div className="panel-title">Signal controls</div>
      <div className="controls-row">
        <div className="control">
          <label htmlFor="seed">Seed</label>
          <input id="seed" type="number" value={form.seed} onChange={setNumber('seed')} />
        </div>
        <div className="control">
          <label htmlFor="owners">Owners</label>
          <input id="owners" type="number" min="1" max="50" value={form.owners}
            onChange={setNumber('owners')} />
        </div>
        <div className="control">
          <label htmlFor="signalsPerOwner">Signals / owner</label>
          <input id="signalsPerOwner" type="number" min="1" max="20" value={form.signalsPerOwner}
            onChange={setNumber('signalsPerOwner')} />
        </div>
        <div className="control">
          <label htmlFor="feedDelayMs">Feed speed (ms)</label>
          <input id="feedDelayMs" type="number" min="0" max="2000" step="50" value={form.feedDelayMs}
            onChange={setNumber('feedDelayMs')} />
        </div>
        <div className="control toggle">
          <input id="hardMode" type="checkbox" checked={form.hardMode}
            onChange={(e) => setForm((f) => ({ ...f, hardMode: e.target.checked }))} />
          <label htmlFor="hardMode">Hard mode</label>
        </div>
        <div className="control-actions">
          <button type="button" className="btn btn-primary" onClick={begin}>
            Begin leads
          </button>
          <button type="button" className="btn" onClick={onReset}>
            Reset
          </button>
        </div>
      </div>
      <div className={`status-chip${running ? ' running' : ''}`}>
        {running
          ? `● Feeding signals${form.hardMode ? ' · hard mode' : ''}`
          : '○ Idle — press Begin to start the feed'}
      </div>
    </section>
  );
}
