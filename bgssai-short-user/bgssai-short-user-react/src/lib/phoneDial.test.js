import test from 'node:test'
import assert from 'node:assert/strict'
import { composeApiPhone, validatePhoneParts } from './phoneDial.js'

test('composeApiPhone formats CN as 11-digit national and others as +E.164', () => {
  assert.equal(composeApiPhone('+86', '138 0013-8000'), '13800138000')
  assert.equal(composeApiPhone('+852', '9123 4567'), '+85291234567')
  assert.equal(composeApiPhone('+1', '415-555-0100'), '+14155550100')
})

test('validatePhoneParts enforces 11-digit CN and 7-15 digit E.164 international', () => {
  assert.equal(validatePhoneParts('+86', '13800138000'), null)
  assert.ok(validatePhoneParts('+86', '12800138000'))
  assert.equal(validatePhoneParts('+852', '91234567'), null)
  assert.equal(validatePhoneParts('+1', '4155550100'), null)
  assert.ok(validatePhoneParts('+1', '1234'))
  assert.ok(validatePhoneParts('+852', '1'.repeat(16)))
})
