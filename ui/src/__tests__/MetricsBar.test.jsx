import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import MetricsBar from '../components/MetricsBar.jsx';

const text = (container) => container.textContent.replace(/\s+/g, ' ');

test('renders nothing when metrics are absent', () => {
  const { container } = render(<MetricsBar metrics={null} />);
  expect(container).toBeEmptyDOMElement();
});

test('renders the metric tiles and tier breakdown', () => {
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
  expect(screen.getByTestId('metric-signals')).toHaveTextContent('48');
  expect(screen.getByTestId('metric-leads')).toHaveTextContent('8');
  expect(screen.getByTestId('metric-p50')).toHaveTextContent('1ms');
  expect(screen.getByTestId('metric-p95')).toHaveTextContent('2ms');
  expect(text(container)).toContain('6 hot');
  expect(text(container)).toContain('2 warm');
  expect(text(container)).toContain('0 cold');
});
