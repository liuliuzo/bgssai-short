import test from 'node:test'
import assert from 'node:assert/strict'
import { buildProviderKeyPayload, formatProviderStatus, hasMaskToken } from './userAiProviderKeysHelper.js'

test('hasMaskToken detects masked placeholders', () => {
  assert.equal(hasMaskToken('sk-a****wxyz'), true)
  assert.equal(hasMaskToken('sk-live-abcdef123456'), false)
})

test('buildProviderKeyPayload trims inputs and validates KLING dual modes and MINIMAX base_url', () => {
  const masked = buildProviderKeyPayload({
    providerCode: 'DEEPSEEK',
    configured: false,
    apiKey: 'sk-a****1234',
  })
  assert.match(masked.error, /真实密钥/)

  const minimaxIo = buildProviderKeyPayload({
    providerCode: 'minimax',
    configured: false,
    apiKey: '  mm-key-999999  ',
    baseUrl: ' https://api.minimax.io ',
    enabled: true,
  })
  assert.equal(minimaxIo.error, undefined)
  assert.deepEqual(minimaxIo.payload, {
    provider_code: 'MINIMAX',
    api_key: 'mm-key-999999',
    base_url: 'https://api.minimax.io',
    enabled: true,
  })

  const klingAkSk = buildProviderKeyPayload({
    providerCode: 'KLING',
    supportsAkSk: true,
    authMode: 'AK_SK',
    configured: false,
    accessKey: ' ak-123 ',
    secretKey: ' sk-456 ',
    baseUrl: 'https://api-beijing.klingai.com',
  })
  assert.equal(klingAkSk.error, undefined)
  assert.deepEqual(klingAkSk.payload, {
    provider_code: 'KLING',
    access_key: 'ak-123',
    secret_key: 'sk-456',
    base_url: 'https://api-beijing.klingai.com',
    enabled: true,
  })

  const klingBearer = buildProviderKeyPayload({
    providerCode: 'KLING',
    supportsAkSk: true,
    authMode: 'API_KEY',
    configured: false,
    apiKey: '  kling-bearer-xyz  ',
    baseUrl: 'https://api.klingai.com',
  })
  assert.equal(klingBearer.error, undefined)
  assert.equal(klingBearer.payload.api_key, 'kling-bearer-xyz')

  assert.equal(formatProviderStatus({ configured: false }), '未配置')
  assert.equal(formatProviderStatus({ configured: true, enabled: true }), '已启用')
  assert.equal(formatProviderStatus({ configured: true, enabled: false }), '已停用')
})

test('buildProviderKeyPayload trims key_name, allows explicit clear, and enforces 64 char max', () => {
  const withName = buildProviderKeyPayload({
    providerCode: 'DEEPSEEK',
    configured: false,
    apiKey: 'sk-live-1234',
    keyName: '  Production Key  ',
  })
  assert.equal(withName.error, undefined)
  assert.equal(withName.payload.key_name, 'Production Key')

  const cleared = buildProviderKeyPayload({
    providerCode: 'DEEPSEEK',
    configured: true,
    keyName: '   ',
  })
  assert.equal(cleared.error, undefined)
  assert.equal(cleared.payload.key_name, '')

  const tooLong = buildProviderKeyPayload({
    providerCode: 'DEEPSEEK',
    configured: true,
    keyName: 'a'.repeat(65),
  })
  assert.match(tooLong.error, /64/)
})
