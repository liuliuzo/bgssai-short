import test from 'node:test'
import assert from 'node:assert/strict'
import { legacyCredentialAbilities, modelKeyMask, modelOptions, modelSelectionFields, modelsForAbility } from './modelKeySelection.js'

test('credentials display only masked values, including both halves of an AK/SK pair', () => {
  assert.equal(modelKeyMask({ api_key_masked: 'sk-****1234' }), 'sk-****1234')
  assert.equal(modelKeyMask({ api_key_masked: 'ak-****1234', secret_key_masked: 'sk-****5678' }), 'ak-****1234 / sk-****5678')
  assert.equal(modelKeyMask({ api_key: 'never-display', secret_key: 'never-display' }), '-')
})

test('duplicate legacy bindings show one model option and select the enabled default as representative', () => {
  const model = { provider_code: 'OPENAI', model_code: 'same-code', ability_type: 'TEXT', platform_model: 0 }
  const rows = [
    { ...model, key_id: 'old-1', credential_id: '01' },
    { ...model, key_id: 'old-2', credential_id: '02', enabled: 1 },
    { ...model, key_id: 'current', credential_id: '03', enabled: 1, default_model: 1 },
    { ...model, key_id: 'old-4', credential_id: '04' },
    { ...model, key_id: 'other-provider', provider_code: 'TENCENT_TOKENHUB' },
    { ...model, key_id: 'other-ability', ability_type: 'IMAGE' },
    { ...model, key_id: 'builtin', platform_model: 1 },
  ]
  const options = modelOptions(rows)
  assert.deepEqual(options.map((row) => row.key_id), ['current', 'other-provider', 'other-ability', 'builtin'])
  assert.equal(rows.length, 7)
})

test('model selection fields contain per-ability metadata independently of provider secrets', () => {
  assert.deepEqual(modelSelectionFields('VOICE', ' fixture ', ' 显示名称 '), {
    modality_type: 'AUDIO', ability_type: 'VOICE', model_code: 'fixture', model_name: '显示名称',
  })
  assert.equal(modelSelectionFields('IMAGE', 'gpt-image', 'Image').modality_type, 'IMAGE')
})

test('voice synthesis, subtitle recognition and music can be filtered independently within audio', () => {
  const rows = ['VOICE', 'ASR', 'MUSIC'].map((ability_type) => ({ modality_type: 'AUDIO', ability_type }))
  for (const row of rows) assert.deepEqual(modelsForAbility(rows, row.ability_type), [row])
  assert.deepEqual(modelsForAbility(rows, 'ALL'), rows)
  assert.deepEqual(modelsForAbility(rows, 'TEXT'), [])
})

test('mixed legacy use identifies only distinct enabled abilities until the provider chooses one credential', () => {
  const creds = [
    { provider_code: 'OPENAI', credential_id: 'a', active: false },
    { provider_code: 'OPENAI', credential_id: 'b', active: false },
    { provider_code: 'MINIMAX', credential_id: 'c', active: true },
  ]
  const text = { provider_code: 'OPENAI', credential_id: 'a', ability_type: 'TEXT', enabled: 1 }
  const models = [
    text, { ...text }, { ...text, ability_type: 'ASR' },
    { ...text, credential_id: 'b', ability_type: 'IMAGE' },
    { ...text, ability_type: 'VIDEO', enabled: 0 },
    { ...text, provider_code: 'MINIMAX', credential_id: 'c' },
    { ...text, ability_type: 'MUSIC', platform_model: 1 },
  ]
  const result = legacyCredentialAbilities(creds, models)
  assert.deepEqual([...result.get('a')], ['TEXT', 'ASR'])
  assert.deepEqual([...result.get('b')], ['IMAGE'])
  assert.equal(result.has('c'), false)
  creds[1].active = true
  assert.equal(legacyCredentialAbilities(creds, models).size, 0)
})
