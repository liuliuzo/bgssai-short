/**
 * Compact token display (REQ-SHORT-COMMON-019).
 * 0 / 999 stay plain; 1000 → 1K; millions → M; billions → B; max one decimal.
 * APIs and exports keep precise integers — this is UI-only.
 */
function normalizeNumber(value) {
  if (value == null || typeof value === 'boolean') return 0
  if (typeof value === 'string') {
    const cleaned = value.replace(/,/g, '').trim()
    if (!cleaned) return 0
    const parsed = Number(cleaned)
    return Number.isFinite(parsed) ? parsed : 0
  }
  const n = Number(value)
  return Number.isFinite(n) ? n : 0
}

export function formatTokenAmount(value) {
  const n = normalizeNumber(value)
  if (n === 0) return '0'
  const abs = Math.trunc(Math.abs(n))
  if (abs === 0) return '0'
  const sign = n < 0 ? '-' : ''
  if (abs < 1000) return sign + String(abs)
  if (abs < 1_000_000) {
    const kRounded = Math.round((abs / 1000) * 10) / 10
    if (kRounded >= 1000) return sign + '1M'
    return sign + formatRounded(kRounded) + 'K'
  }
  if (abs < 1_000_000_000) {
    const mRounded = Math.round((abs / 1_000_000) * 10) / 10
    if (mRounded >= 1000) return sign + '1B'
    return sign + formatRounded(mRounded) + 'M'
  }
  const bRounded = Math.round((abs / 1_000_000_000) * 10) / 10
  return sign + formatRounded(bRounded) + 'B'
}

export function formatTokenExact(value) {
  const n = normalizeNumber(value)
  const truncated = Math.trunc(n)
  if (Object.is(truncated, -0)) return '0'
  return truncated.toLocaleString('en-US')
}

export function formatTokenWithHover(value) {
  const n = Math.trunc(normalizeNumber(value))
  return {
    compact: formatTokenAmount(n),
    exact: formatTokenExact(n),
    raw: Object.is(n, -0) ? 0 : n,
  }
}

function formatRounded(rounded) {
  return Number.isInteger(rounded) ? String(rounded) : rounded.toFixed(1)
}

export const formatToken = formatTokenAmount
export default formatTokenAmount


