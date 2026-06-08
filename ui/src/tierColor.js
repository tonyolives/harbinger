// Shared tier → color map, tuned to pop as accent stripes / badges on the dark command-center theme.
export const TIER_COLOR = {
  HOT: '#ff4d4f',
  WARM: '#ffa940',
  COLD: '#8794a8',
};

export function tierColor(tier) {
  return TIER_COLOR[tier] || TIER_COLOR.COLD;
}
