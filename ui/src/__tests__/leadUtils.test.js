import { rank, summarize } from '../leadUtils.js';

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

const metrics = {
  signalsProcessed: 48,
  leadsSurfaced: 8,
  hotCount: 2,
  warmCount: 4,
  coldCount: 2,
  signalToLeadP50Ms: 40,
  signalToLeadP95Ms: 90,
};

test('summarize derives entity-resolution, quality, speed, and latency numbers', () => {
  const leads = [lead('a', 80), lead('b', 60)];
  const s = summarize(metrics, leads, 12000);

  expect(s.consolidation).toBe(6); // 48 / 8
  expect(s.hotRatePct).toBe(25); // 2 / 8
  expect(s.actionable).toBe(6); // hot + warm
  expect(s.actionablePct).toBe(75); // 6 / 8
  expect(s.avgScore).toBe(70); // (80 + 60) / 2
  expect(s.durationSec).toBe(12);
  expect(s.throughput).toBe(4); // 48 / 12
  expect(s.p50Ms).toBe(40);
  expect(s.p95Ms).toBe(90);
  expect(s.tiers.warm).toEqual({ count: 4, pct: 50 });
});

test('summarize guards divide-by-zero on an empty / instantaneous run', () => {
  const s = summarize(
    { ...metrics, leadsSurfaced: 0, hotCount: 0, warmCount: 0, coldCount: 0 },
    [],
    0,
  );
  expect(s.consolidation).toBe(0);
  expect(s.hotRatePct).toBe(0);
  expect(s.avgScore).toBe(0);
  expect(s.throughput).toBe(0);
  expect(Number.isNaN(s.consolidation)).toBe(false);
});
