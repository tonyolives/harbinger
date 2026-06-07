import '@testing-library/jest-dom';
import { render, screen, act } from '@testing-library/react';
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
  expect(await screen.findByText(/signals 10/)).toBeInTheDocument();
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
