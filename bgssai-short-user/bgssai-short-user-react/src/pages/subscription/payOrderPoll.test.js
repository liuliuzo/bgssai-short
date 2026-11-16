import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  isPayOrderExpired,
  isPollWindowExceeded,
  isTerminalPayStatus,
  PAY_ORDER_POLL_MAX_MS,
  parseExpiresAtMs,
  resolvePollOutcome,
} from './payOrderPoll.js'

describe('payOrderPoll', () => {
  it('treats paid and closed as terminal', () => {
    assert.equal(isTerminalPayStatus('paid'), true)
    assert.equal(isTerminalPayStatus('closed'), true)
    assert.equal(isTerminalPayStatus('created'), false)
  })

  it('parses expires_at with space separator', () => {
    const ms = parseExpiresAtMs('2026-08-14 12:30:00')
    assert.ok(ms != null)
    assert.equal(new Date(ms).getUTCFullYear(), 2026)
  })

  it('parses naive wall-clock expires_at as Asia/Shanghai regardless of browser zone', () => {
    const ms = parseExpiresAtMs('2026-08-14 12:30:00')
    assert.equal(ms, Date.UTC(2026, 7, 14, 4, 30, 0))
    assert.equal(parseExpiresAtMs('2026-08-14T12:30:00'), Date.UTC(2026, 7, 14, 4, 30, 0))
  })

  it('keeps explicit zone offsets as given', () => {
    assert.equal(
      parseExpiresAtMs('2026-08-14T12:30:00+09:00'),
      Date.UTC(2026, 7, 14, 3, 30, 0),
    )
    assert.equal(
      parseExpiresAtMs('2026-08-14T12:30:00Z'),
      Date.UTC(2026, 7, 14, 12, 30, 0),
    )
  })

  it('bounds the local poll window when expires_at is missing', () => {
    const start = Date.UTC(2026, 7, 14, 0, 0, 0)
    assert.equal(isPollWindowExceeded(start, start + PAY_ORDER_POLL_MAX_MS - 1), false)
    assert.equal(isPollWindowExceeded(start, start + PAY_ORDER_POLL_MAX_MS), true)
    assert.equal(isPollWindowExceeded(null, start + PAY_ORDER_POLL_MAX_MS), false)
  })

  it('flags expired orders', () => {
    assert.equal(isPayOrderExpired('2020-01-01 00:00:00', Date.parse('2021-01-01T00:00:00')), true)
    assert.equal(isPayOrderExpired('2099-01-01 00:00:00', Date.parse('2021-01-01T00:00:00')), false)
  })

  it('resolves poll outcomes', () => {
    assert.equal(resolvePollOutcome('paid', null), 'paid')
    assert.equal(resolvePollOutcome('closed', null), 'closed')
    assert.equal(
      resolvePollOutcome('created', '2020-01-01 00:00:00', Date.parse('2021-01-01T00:00:00')),
      'timeout',
    )
    assert.equal(
      resolvePollOutcome('created', '2099-01-01 00:00:00', Date.parse('2021-01-01T00:00:00')),
      'continue',
    )
  })
})
