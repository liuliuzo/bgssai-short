/**
 * Compact token display (REQ-SHORT-COMMON-019).
 * 0 / 999 stay plain; 1000 → 1K; millions → M; billions → B; max one decimal.
 * APIs and exports keep precise integers — this is UI-only.
 */
export function formatTokenAmount(value) {
  const n = Number(value)
  if (!Number.isFinite(n) || n === 0) return '0'
  const abs = Math.abs(n)
  const sign = n < 0 ? '-' : ''
  if (abs < 1000) return sign + String(Math.trunc(abs))
  if (abs < 1_000_000) return sign + trimOne(abs / 1000) + 'K'
  if (abs < 1_000_000_000) return sign + trimOne(abs / 1_000_000) + 'M'
  return sign + trimOne(abs / 1_000_000_000) + 'B'
}

export function formatTokenExact(value) {
  const n = Number(value)
  if (!Number.isFinite(n)) return '0'
  return Math.trunc(n).toLocaleString('en-US')
}

function trimOne(v) {
  const rounded = Math.round(v * 10) / 10
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1)
}

export default formatTokenAmount
