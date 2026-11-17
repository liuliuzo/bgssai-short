import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { formatKeyBalance } from './keyBalance.js'

describe('external api key balance', () => {
  it('shows provider amount and unit from the list payload', () => {
    assert.equal(formatKeyBalance({
      balance_status: 'OK',
      balance_list: [
        { label: '月度', amount: '0', unit: '积分' },
        { label: '资源包', amount: '10000', unit: '积分' },
      ],
    }), '月度 0 积分 · 资源包 10000 积分')
    assert.equal(formatKeyBalance({
      balance_status: 'OK',
      balance_list: [{ label: '', amount: '110.00', unit: '元' }],
    }), '110.00 元')
    assert.equal(formatKeyBalance({
      balance_status: 'OK',
      balance_list: [{ label: '视频包', amount: '118', unit: '次' }],
    }), '视频包 118 次')
  })

  it('does not invent a number when the provider has no balance read', () => {
    assert.equal(formatKeyBalance({ balance_status: 'UNAVAILABLE', balance_list: [] }), '-')
    assert.equal(formatKeyBalance({ balance_status: 'FAILED', balance_list: [] }), '查询失败')
    assert.equal(formatKeyBalance({}), '-')
    assert.equal(formatKeyBalance({ balance_status: 'OK', balance_list: [] }), '-')
  })
})
