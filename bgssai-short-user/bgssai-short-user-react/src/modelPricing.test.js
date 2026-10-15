import test from 'node:test'
import assert from 'node:assert/strict'
import { modelPricingLabel, receiptSummary } from './modelPricing.js'
test('real fractional multipliers and version appear without guessing',()=>{
 for(const multiplier of [0.5,1,1.5]) assert.match(modelPricingLabel({model:'actual',billing_multiplier:multiplier,billing_unit:'token',price_version:7}),new RegExp(String(multiplier).replace('.',String.fromCharCode(92)+'.')+'× / token'))
 assert.match(modelPricingLabel({model:'gpt-any'}),/倍率待确认/)
})
test('receipt displays billed amount once, unknown stays pending',()=>{
 assert.match(receiptSummary({raw_tokens:100,billed_tokens:150,billing_multiplier:1.5,billing_status:'unpriced'}),/实扣 150/)
 assert.doesNotMatch(receiptSummary({}),/实扣 0/)
 assert.match(receiptSummary({debit_fen:3,currency:'USD'}),/3 USD/)
})
