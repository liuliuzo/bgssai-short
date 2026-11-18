export function activateNewProviderKey(keys, providerCode) {
  return Array.isArray(keys) && !keys.some((row) => Number(row.platform_model) !== 1 && row.provider_code === providerCode)
}

export function modelKeyMask(row) {
  return [row.api_key_masked, row.secret_key_masked].filter(Boolean).join(' / ') || '-'
}
