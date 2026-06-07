import '@testing-library/jest-dom';
import { render } from '@testing-library/react';
import MetricsBar from '../components/MetricsBar.jsx';

const text = (container) => container.textContent.replace(/\s+/g, ' ');

test('renders nothing when metrics are absent', () => {
  const { container } = render(<MetricsBar metrics={null} />);
  expect(container).toBeEmptyDOMElement();
});

test('renders the metric values and tier breakdown', () => {
  const { container } = render(
    <MetricsBar
      metrics={{
        signalsProcessed: 48,
        leadsSurfaced: 8,
        hotCount: 6,
        warmCount: 2,
        coldCount: 0,
        signalToLeadP50Ms: 1,
        signalToLeadP95Ms: 2,
      }}
    />,
  );
  expect(text(container)).toContain('signals 48');
  expect(text(container)).toContain('leads 8');
  expect(text(container)).toContain('p50 1ms');
  expect(text(container)).toContain('p95 2ms');
  expect(text(container)).toContain('6 hot');
  expect(text(container)).toContain('2 warm');
  expect(text(container)).toContain('0 cold');
});
