import test from 'node:test'
import assert from 'node:assert/strict'
import { activateNewProviderKey, modelKeyMask } from './modelKeySelection.js'

test('additional model keys start on standby even if the existing provider key is disabled or another modality', () => {
  const rows = [
    { provider_code: 'OPENAI', modality_type: 'IMAGE', enabled: 0, platform_model: 0 },
    { provider_code: 'BGSSAI', modality_type: 'TEXT', enabled: 1, platform_model: 1 },
  ]
  assert.equal(activateNewProviderKey(rows, 'OPENAI'), false)
  assert.equal(activateNewProviderKey(rows, 'DEEPSEEK'), true)
  assert.equal(activateNewProviderKey([], 'OPENAI'), true)
  assert.equal(activateNewProviderKey(null, 'OPENAI'), false)
})

test('legacy unnamed and dual-key rows display only the masked credentials returned by the list', () => {
  assert.equal(modelKeyMask({ api_key_masked: 'sk-****1234' }), 'sk-****1234')
  assert.equal(modelKeyMask({ api_key_masked: 'ak-****1234', secret_key_masked: 'sk-****5678' }), 'ak-****1234 / sk-****5678')
  assert.equal(modelKeyMask({ api_key: 'never-display', secret_key: 'never-display' }), '-')
})
