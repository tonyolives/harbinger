import '@testing-library/jest-dom';
import { render, screen, act, fireEvent } from '@testing-library/react';
import App from '../App.jsx';

const lead = (id, name, score, tier) => ({
  homeownerId: id,
  name,
  address: '1 Main St',
  intentScore: score,
  tier,
  reasons: ['Pre-foreclosure filing'],
  explanation: `${tier} lead (score ${score})`,
});

const metrics = {
  signalsProcessed: 10,
  leadsSurfaced: 1,
  hotCount: 1,
  warmCount: 0,
  coldCount: 0,
  signalToLeadP50Ms: 1,
  signalToLeadP95Ms: 1,
};

// Mutable backend state the mocked fetch reads; tests change it then fire a `lead` event, which
// drives the hook to re-fetch (matching the real notify-then-refetch flow).
let leadsBody;

// Minimal EventSource stand-in: jsdom has none.
class FakeEventSource {
  constructor(url) {
    this.url = url;
    this.listeners = {};
    FakeEventSource.instance = this;
  }
  addEventListener(type, cb) {
    this.listeners[type] = cb;
  }
  emit(type, data) {
    this.listeners[type]?.({ data: JSON.stringify(data ?? {}) });
  }
  close() {
    this.closed = true;
  }
}

beforeEach(() => {
  leadsBody = [lead('a', 'owen purdy', 75, 'HOT')];
  global.EventSource = FakeEventSource;
  global.fetch = jest.fn((url) => {
    const body = url.includes('/metrics') ? metrics : leadsBody;
    return Promise.resolve({ json: () => Promise.resolve(body) });
  });
});

afterEach(() => {
  jest.resetAllMocks();
  delete global.EventSource;
});

test('loads the initial ranked leads and metrics', async () => {
  render(<App />);
  expect(await screen.findByText('owen purdy')).toBeInTheDocument();
  expect(await screen.findByTestId('metric-signals')).toHaveTextContent('10');
});

test('a lead event triggers a refetch that shows the new ranked state', async () => {
  render(<App />);
  await screen.findByText('owen purdy');

  // Backend now has a higher-scoring homeowner; the event tells the UI to re-read.
  leadsBody = [lead('b', 'jean pollich', 100, 'HOT'), lead('a', 'owen purdy', 75, 'HOT')];
  await act(async () => {
    FakeEventSource.instance.emit('lead');
  });

  const items = await screen.findAllByRole('listitem');
  expect(items).toHaveLength(2);
  expect(items[0]).toHaveTextContent('jean pollich'); // 100 outranks 75
  expect(items[1]).toHaveTextContent('owen purdy');
});

test('refetch reflects an updated score for the same homeowner', async () => {
  render(<App />);
  await screen.findByText('owen purdy');

  leadsBody = [lead('a', 'owen purdy', 95, 'HOT')];
  await act(async () => {
    FakeEventSource.instance.emit('lead');
  });

  const items = await screen.findAllByRole('listitem');
  expect(items).toHaveLength(1);
  expect(items[0]).toHaveTextContent('HOT 95');
});

// The last signals of a run can be no-ops (they change no lead's score or reasons), so they publish
// no `lead` event and the client's metrics are left a step behind the backend. On completion the
// hook must re-read the authoritative final state so the run summary's count is the true total, not
// whatever the last event happened to carry.
test('on idle completion, re-reads final backend state so the summary is not left stale', async () => {
  jest.useFakeTimers();
  try {
    // Backend reports 46 processed during the run; the final two no-op signals land afterward, so
    // the true total (48) only appears on a re-read — never on a `lead` event.
    let signalsProcessedNow = 46;
    const metricsBody = () => ({
      signalsProcessed: signalsProcessedNow,
      leadsSurfaced: 8,
      hotCount: 6,
      warmCount: 2,
      coldCount: 0,
      signalToLeadP50Ms: 0,
      signalToLeadP95Ms: 6,
    });
    const eightLeads = Array.from({ length: 8 }, (_, i) =>
      lead(String(i), `owner ${i}`, 100 - i, 'HOT'),
    );
    global.fetch = jest.fn((url) => {
      if (url.includes('/metrics')) {
        return Promise.resolve({ json: () => Promise.resolve(metricsBody()) });
      }
      if (url.includes('/demo/start')) {
        return Promise.resolve({});
      }
      return Promise.resolve({ json: () => Promise.resolve(eightLeads) });
    });

    render(<App />);
    await act(async () => {}); // flush the initial load

    // Begin the feed (ControlPanel defaults: 8 owners × 6 signals = 48 expected).
    await act(async () => {
      fireEvent.click(screen.getByText('Begin leads'));
    });

    // A mid-run event: the client now sees 46 (< 48), so it arms the idle-completion timer.
    await act(async () => {
      FakeEventSource.instance.emit('lead');
    });

    // The final two no-op signals land on the backend without publishing an event.
    signalsProcessedNow = 48;

    // Idle timeout fires completion; finish() re-reads and corrects the summary to the true total.
    await act(async () => {
      jest.advanceTimersByTime(2000);
    });
    await act(async () => {}); // flush the completion re-read

    expect(screen.getByTestId('summary-leads')).toHaveTextContent('48 → 8');
    expect(screen.getByTestId('summary-consolidation')).toHaveTextContent('6.0×');
  } finally {
    jest.useRealTimers();
  }
});
