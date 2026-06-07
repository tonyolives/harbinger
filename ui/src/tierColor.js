// Shared tier → color map (intentionally plain; Phase 9 will design the real palette).
export const TIER_COLOR = {
  HOT: '#c0392b',
  WARM: '#e67e22',
  COLD: '#7f8c8d',
};

export function tierColor(tier) {
  return TIER_COLOR[tier] || TIER_COLOR.COLD;
}
