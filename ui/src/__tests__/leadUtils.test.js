import { rank } from '../leadUtils.js';

const lead = (id, score) => ({ homeownerId: id, intentScore: score });

test('rank sorts by intentScore descending', () => {
  const ranked = rank([lead('a', 40), lead('b', 90), lead('c', 75)]);
  expect(ranked.map((l) => l.intentScore)).toEqual([90, 75, 40]);
});

test('rank does not mutate the input', () => {
  const input = [lead('a', 40), lead('b', 90)];
  rank(input);
  expect(input.map((l) => l.intentScore)).toEqual([40, 90]);
});
