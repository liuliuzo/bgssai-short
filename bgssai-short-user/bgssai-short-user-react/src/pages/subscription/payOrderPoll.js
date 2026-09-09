/** Terminal payment_order statuses that stop client polling. */
export const TERMINAL_PAY_STATUSES = Object.freeze(['paid', 'closed'])

/**
 * @param {string|null|undefined} status
 * @returns {boolean}
 */
export function isTerminalPayStatus(status) {
  return TERMINAL_PAY_STATUSES.includes(status)
}

/**
 * Parse server expires_at (`yyyy-MM-dd HH:mm:ss`) to epoch ms.
 * @param {string|null|undefined} expiresAt
 * @returns {number|null}
 */
export function parseExpiresAtMs(expiresAt) {
  if (!expiresAt || typeof expiresAt !== 'string') return null
  const normalized = expiresAt.trim().includes('T')
    ? expiresAt.trim()
    : expiresAt.trim().replace(' ', 'T')
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
