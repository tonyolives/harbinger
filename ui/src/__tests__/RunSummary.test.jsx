import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import RunSummary from '../components/RunSummary.jsx';

const metrics = {
  signalsProcessed: 48,
  leadsSurfaced: 8,
  hotCount: 2,
  warmCount: 4,
  coldCount: 2,
  signalToLeadP50Ms: 40,
  signalToLeadP95Ms: 90,
};
const leads = [
  { homeownerId: 'a', intentScore: 80 },
  { homeownerId: 'b', intentScore: 60 },
];

test('renders nothing until the run is done', () => {
  const { container } = render(
    <RunSummary done={false} metrics={metrics} leads={leads} elapsedMs={12000} />,
  );
  expect(container).toBeEmptyDOMElement();
});

test('renders nothing when metrics are absent', () => {
  const { container } = render(
    <RunSummary done metrics={null} leads={leads} elapsedMs={12000} />,
  );
  expect(container).toBeEmptyDOMElement();
});

test('renders the headline, all metric groups, and the tier bar when done', () => {
  render(<RunSummary done metrics={metrics} leads={leads} elapsedMs={12000} />);

  // Headline narrative.
  expect(screen.getByTestId('run-summary')).toHaveTextContent(
    'Resolved 48 messy signals into 8 unique homeowner leads',
  );

  // Entity resolution + speed + quality + latency tiles.
  expect(screen.getByTestId('summary-consolidation')).toHaveTextContent('6.0×');
  expect(screen.getByTestId('summary-leads')).toHaveTextContent('48 → 8');
  expect(screen.getByTestId('summary-hot-rate')).toHaveTextContent('25%');
  expect(screen.getByTestId('summary-actionable')).toHaveTextContent('6 (75%)');
  expect(screen.getByTestId('summary-avg-score')).toHaveTextContent('70');
  expect(screen.getByTestId('summary-duration')).toHaveTextContent('12.0s');
  expect(screen.getByTestId('summary-throughput')).toHaveTextContent('4.0/s');
  expect(screen.getByTestId('summary-latency')).toHaveTextContent('40 / 90ms');

  // Tier bar renders a segment per non-empty tier (hot, warm, cold all > 0 here).
  const bar = screen.getByTestId('tier-bar');
  expect(bar.querySelectorAll('.tier-bar-seg')).toHaveLength(3);
});
