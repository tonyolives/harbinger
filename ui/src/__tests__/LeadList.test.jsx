import '@testing-library/jest-dom';
import { render, screen } from '@testing-library/react';
import LeadList from '../components/LeadList.jsx';
import { TIER_COLOR } from '../tierColor.js';

const lead = (id, name, score, tier) => ({
  homeownerId: id,
  name,
  address: '1 Main St',
  intentScore: score,
  tier,
  reasons: ['Pre-foreclosure filing'],
  explanation: `${tier} lead`,
});

test('shows an empty state when there are no leads', () => {
  render(<LeadList leads={[]} />);
  expect(screen.getByText(/no leads yet/i)).toBeInTheDocument();
});

test('renders one row per lead in the given order', () => {
  render(<LeadList leads={[lead('a', 'owen', 90, 'HOT'), lead('b', 'dana', 75, 'HOT')]} />);
  const items = screen.getAllByRole('listitem');
  expect(items).toHaveLength(2);
  expect(items[0]).toHaveTextContent('owen');
  expect(items[1]).toHaveTextContent('dana');
});

test('colors the tier badge by tier (HOT and WARM)', () => {
  render(<LeadList leads={[lead('a', 'owen', 90, 'HOT'), lead('b', 'terry', 59, 'WARM')]} />);
  const badges = screen.getAllByTestId('tier-badge');
  expect(badges[0]).toHaveStyle({ backgroundColor: TIER_COLOR.HOT });
  expect(badges[0]).toHaveTextContent('HOT 90');
  expect(badges[1]).toHaveStyle({ backgroundColor: TIER_COLOR.WARM });
  expect(badges[1]).toHaveTextContent('WARM 59');
});
