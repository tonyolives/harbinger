// Pure helper for the lead list. The backend already returns ranked leads; this re-sorts
// defensively so the UI never depends on response ordering.

/** Strongest first (highest intent score). Returns a new array. */
export function rank(leads) {
  return [...leads].sort((a, b) => b.intentScore - a.intentScore);
}
