import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { safeReturnPath } from './safeReturn.js'
import { yuanToCent, centToYuanNumber } from './money.js'
import { formatDateTime } from './formatTime.js'

describe('safeReturnPath', () => {
  it('keeps in-app paths and drops open redirects', () => {
    assert.equal(safeReturnPath('/reviews?status=PENDING'), '/reviews?status=PENDING')
    assert.equal(safeReturnPath('//evil.example'), null)
    assert.equal(safeReturnPath('/login?redirect=/home'), null)
  })
})

describe('money', () => {
  it('converts yuan inputs to integer cents', () => {
    assert.equal(yuanToCent(0.5), 50)
    assert.equal(yuanToCent(1.01), 101)
    assert.equal(centToYuanNumber(13700), 137)
  })
})

describe('formatDateTime', () => {
  it('renders epoch millis in Asia/Shanghai and leaves wall strings', () => {
    assert.equal(formatDateTime(1792419110000), '2026-10-19 22:11:50')
    assert.equal(formatDateTime('2026-10-16 15:29:08'), '2026-10-16 15:29:08')
  })
})
