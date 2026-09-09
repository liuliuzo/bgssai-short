import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  isPayOrderExpired,
  isTerminalPayStatus,
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
