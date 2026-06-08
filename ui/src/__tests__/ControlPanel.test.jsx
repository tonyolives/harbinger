import '@testing-library/jest-dom';
import { fireEvent, render, screen } from '@testing-library/react';
import ControlPanel from '../components/ControlPanel.jsx';

test('Begin calls onStart with the current form values', () => {
  const onStart = jest.fn();
  render(<ControlPanel running={false} onStart={onStart} onReset={() => {}} />);

  fireEvent.change(screen.getByLabelText('Seed'), { target: { value: '7' } });
  fireEvent.change(screen.getByLabelText('Owners'), { target: { value: '5' } });
  fireEvent.click(screen.getByLabelText('Hard mode'));
  fireEvent.click(screen.getByRole('button', { name: /begin leads/i }));

  expect(onStart).toHaveBeenCalledTimes(1);
  expect(onStart).toHaveBeenCalledWith(
    expect.objectContaining({ seed: 7, owners: 5, hardMode: true }),
  );
});

test('Reset calls onReset', () => {
  const onReset = jest.fn();
  render(<ControlPanel running onStart={() => {}} onReset={onReset} />);

  fireEvent.click(screen.getByRole('button', { name: /reset/i }));
  expect(onReset).toHaveBeenCalledTimes(1);
});

test('shows running status when feeding', () => {
  render(<ControlPanel running onStart={() => {}} onReset={() => {}} />);
  expect(screen.getByText(/feeding signals/i)).toBeInTheDocument();
});
