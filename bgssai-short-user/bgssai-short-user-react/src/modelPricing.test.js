import test from 'node:test'
import assert from 'node:assert/strict'
import {
  modelPricingLabel,
  receiptSummary,
  isValidBillingMultiplier,
  validateTokenhubCnRoute,
  detectMultiplierDrift,
  estimateShotBatchTokens,
} from './modelPricing.js'
import { formatToken, formatTokenExact, formatTokenWithHover } from './utils/formatToken.js'
import {
  ASPECT_RATIOS,
  RHYTHM_MARKERS,
  evaluateShotReadiness,
  validateTubePublishPreflight,
} from './utils/CinematographyHelper.js'

test('real fractional multipliers and version appear without guessing', () => {
  for (const multiplier of [0.5, 1, 1.5]) {
    assert.match(
      modelPricingLabel({ model: 'actual', billing_multiplier: multiplier, billing_unit: 'token', price_version: 7 }),
      new RegExp(String(multiplier).replace('.', String.fromCharCode(92) + '.') + '×'),
    )
  }
  assert.match(modelPricingLabel({ model: 'gpt-any' }), /按任务实际对账/)
  assert.doesNotMatch(modelPricingLabel({ model: 'gpt-any' }), /倍率待确认/)
  const branded = modelPricingLabel({ model: 'TokenHub DeepSeek', billing_multiplier: 1.5 })
  assert.match(branded, /1\.5×/)
  assert.doesNotMatch(branded, /tokenhub/i)
})

test('ZH chrome: fen/million and receipt default unit use 额度 not bare English token', () => {
  assert.match(
    modelPricingLabel({
      model: 'm',
      price_unit: 'token',
      input_fen_per_million: 100,
      output_fen_per_million: 200,
      currency: 'CNY',
    }),
    /元\/百万额度/,
  )
  assert.doesNotMatch(
    modelPricingLabel({
      model: 'm',
      price_unit: 'token',
      input_fen_per_million: 100,
      output_fen_per_million: 200,
    }),
    /百万 token/,
  )
  assert.match(receiptSummary({ billed_tokens: 150 }), /实扣 150 额度/)
  // Explicit API billing_unit still passed through
  assert.match(receiptSummary({ billed_tokens: 10, billing_unit: 'token' }), /实扣 10 token/)
})

test('receipt displays billed amount once, unknown stays pending, fen shown as yuan', () => {
  assert.match(receiptSummary({ raw_tokens: 100, billed_tokens: 150, billing_multiplier: 1.5, billing_status: 'unpriced' }), /1\.5×/)
  assert.match(receiptSummary({ raw_tokens: 100, billed_tokens: 150, billing_multiplier: 1.5, billing_status: 'unpriced' }), /实扣 150/)
  assert.doesNotMatch(receiptSummary({ billing_multiplier: 1, billed_tokens: 1 }), /tokenhub/i)
  assert.match(receiptSummary({ billing_status: 'unpriced' }), /未定价/)
  assert.doesNotMatch(receiptSummary({}), /实扣 0/)
  assert.match(receiptSummary({ debit_fen: 300, currency: 'CNY' }), /3\.00 元/)
  assert.doesNotMatch(receiptSummary({ debit_fen: 300 }), / 分/)
})

test('formatToken handles K/M/B boundary promotion and safe inputs', () => {
  assert.equal(formatToken(null), '0')
  assert.equal(formatToken(false), '0')
  assert.equal(formatToken(999), '999')
  assert.equal(formatToken(1000), '1K')
  assert.equal(formatToken(1500), '1.5K')
  assert.equal(formatToken(999950), '1M')
  assert.equal(formatToken(999950000), '1B')
  assert.equal(formatToken('1,250,000'), '1.3M')
  assert.equal(formatTokenExact('1250000'), '1,250,000')
  const pair = formatTokenWithHover(1250000)
  assert.equal(pair.compact, '1.3M')
  assert.equal(pair.exact, '1,250,000')
})

test('tokenhub CN route and multiplier drift validation', () => {
  assert.equal(isValidBillingMultiplier(0.5), true)
  assert.equal(isValidBillingMultiplier(1.0), true)
  assert.equal(isValidBillingMultiplier(1.5), true)
  assert.equal(isValidBillingMultiplier(2.0), false)

  assert.equal(validateTokenhubCnRoute('https://api.tokenhub.cn/v1').valid, true)
  assert.equal(validateTokenhubCnRoute('https://api.openai.com/v1').valid, false)
  const overseas = validateTokenhubCnRoute('https://www.bgssai-tokenhub.com/v1')
  assert.equal(overseas.valid, false)
  assert.doesNotMatch(overseas.reason, /tokenhub/i)

  const okDrift = detectMultiplierDrift({ raw_tokens: 1000, billed_tokens: 1500, billing_multiplier: 1.5 })
  assert.equal(okDrift.drifted, false)
  const badDrift = detectMultiplierDrift(
    { billing_multiplier: 1.5, endpoint: 'https://api.tokenhub.cn/v1' },
    { billing_multiplier: 1, endpoint: 'https://api.tokenhub.cn/v1' },
  )
  assert.equal(badDrift.drifted, true)

  const batch = estimateShotBatchTokens([{ shot_id: 's1' }, { shot_id: 's2' }], 1000, 1.5)
  assert.equal(batch.shot_count, 2)
  assert.equal(batch.raw_tokens, 2000)
  assert.equal(batch.billed_tokens, 3000)
})

test('cinematography shot readiness and Tube preflight validation', () => {
  assert.ok(ASPECT_RATIOS.some((a) => a.value === '9:16'))
  assert.ok(RHYTHM_MARKERS.some((r) => r.key === 'HOOK_3S'))

  const unready = evaluateShotReadiness({ previz_text: '' }, {})
  assert.equal(unready.ready, false)

  const readyShot = evaluateShotReadiness(
    {
      previz_text: '林晚推门进入宴会厅',
      character_ref_json: ['c1'],
      scene_card_id: 'sc1',
    },
    {
      character_list: [{ character_id: 'c1', lock_flag: 1 }],
      scene_list: [{ scene_card_id: 'sc1', lock_flag: 1 }],
    },
  )
  assert.equal(readyShot.ready, true)

  assert.equal(
    validateTubePublishPreflight(
      { film_id: 'f1', compose_status: 'READY', expired_flag: 0, film_duration_sec: 65 },
      { review_status: 'APPROVED' },
    ).ready,
    true,
  )
  assert.equal(
    validateTubePublishPreflight(
      { film_id: 'f1', compose_status: 'READY', expired_flag: 0, film_duration_sec: 0 },
      { review_status: 'APPROVED' },
    ).ready,
    false,
  )
})
