/** Terminal payment_order statuses that stop client polling. */
export const TERMINAL_PAY_STATUSES = Object.freeze(['paid', 'closed'])

/**
 * @param {string|null|undefined} status
 * @returns {boolean}
 */
export function isTerminalPayStatus(status) {
  return TERMINAL_PAY_STATUSES.includes(status)
}

const NAIVE_WALL_CLOCK = /^(\d{4})-(\d{2})-(\d{2})[ T](\d{2}):(\d{2}):(\d{2})(\.\d{1,3})?$/
const SHANGHAI_OFFSET_MS = 8 * 60 * 60 * 1000

/**
 * Parse server expires_at (`yyyy-MM-dd HH:mm:ss`) to epoch ms.
 * The server emits Asia/Shanghai wall-clock strings without a zone; parse
 * those as Shanghai time instead of the browser's local zone (same
 * convention as lib/formatTime.js). Strings carrying an explicit zone are
 * taken as given.
 * @param {string|null|undefined} expiresAt
 * @returns {number|null}
 */
export function parseExpiresAtMs(expiresAt) {
  if (!expiresAt || typeof expiresAt !== 'string') return null
  const text = expiresAt.trim()
  const naive = text.match(NAIVE_WALL_CLOCK)
  if (naive) {
    const [, y, mo, d, h, mi, s, frac] = naive
    const fracMs = frac ? Math.round(Number(`0${frac}`) * 1000) : 0
    return Date.UTC(Number(y), Number(mo) - 1, Number(d), Number(h), Number(mi), Number(s), fracMs)
      - SHANGHAI_OFFSET_MS
  }
  const normalized = text.includes('T') ? text : text.replace(' ', 'T')
  const ms = Date.parse(normalized)
  return Number.isNaN(ms) ? null : ms
}

/**
 * @param {string|null|undefined} expiresAt
 * @param {number} [nowMs]
 * @returns {boolean}
 */
export function isPayOrderExpired(expiresAt, nowMs = Date.now()) {
  const ms = parseExpiresAtMs(expiresAt)
  if (ms == null) return false
  return nowMs >= ms
}

/**
 * Decide poll loop outcome from status + optional expires_at.
 * @param {string|null|undefined} status
 * @param {string|null|undefined} expiresAt
 * @param {number} [nowMs]
 * @returns {'paid'|'closed'|'timeout'|'continue'}
 */
export function resolvePollOutcome(status, expiresAt, nowMs = Date.now()) {
  if (status === 'paid') return 'paid'
  if (status === 'closed') return 'closed'
  if (isPayOrderExpired(expiresAt, nowMs)) return 'timeout'
  return 'continue'
}

export const PAY_ORDER_POLL_INTERVAL_MS = 2000

/** Local stop bound used only when the order carries no expires_at. */
export const PAY_ORDER_POLL_MAX_MS = 30 * 60 * 1000

/**
 * @param {number|null|undefined} startedAtMs when this modal started polling
 * @param {number} [nowMs]
 * @returns {boolean}
 */
export function isPollWindowExceeded(startedAtMs, nowMs = Date.now()) {
  if (startedAtMs == null || !Number.isFinite(startedAtMs)) return false
  return nowMs - startedAtMs >= PAY_ORDER_POLL_MAX_MS
}
