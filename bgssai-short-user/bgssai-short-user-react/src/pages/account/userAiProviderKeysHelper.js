export function hasMaskToken(value) {
  if (typeof value !== 'string') {
    return false
  }
  return value.includes('****') || value.includes('••••')
}

export function trimText(value) {
  return typeof value === 'string' ? value.trim() : ''
}

export function buildProviderKeyPayload({
  providerCode,
  authMode = 'API_KEY',
  supportsAkSk = false,
  configured = false,
  apiKey = '',
  accessKey = '',
  secretKey = '',
  baseUrl = '',
  enabled = true,
}) {
  const code = trimText(providerCode).toUpperCase()
  if (!code) {
    return { error: '请选择厂商' }
  }

  const trimmedApiKey = trimText(apiKey)
  const trimmedAk = trimText(accessKey)
  const trimmedSk = trimText(secretKey)
  const trimmedBaseUrl = trimText(baseUrl)

  if (hasMaskToken(trimmedApiKey) || hasMaskToken(trimmedAk) || hasMaskToken(trimmedSk)) {
    return { error: '请填写完整真实密钥' }
  }

  if (trimmedBaseUrl && !trimmedBaseUrl.toLowerCase().startsWith('https://')) {
    return { error: '接口地址需以 https:// 开头' }
  }

  const useAkSk = Boolean(supportsAkSk && authMode === 'AK_SK')
  const payload = {
    provider_code: code,
    base_url: trimmedBaseUrl,
    enabled: Boolean(enabled),
  }

  if (useAkSk) {
    if (!configured && (!trimmedAk || !trimmedSk)) {
      return { error: '请填写完整的 Access Key 与 Secret Key' }
    }
    if ((trimmedAk && !trimmedSk) || (!trimmedAk && trimmedSk)) {
      if (!configured) {
        return { error: '请同时填写 Access Key 与 Secret Key' }
      }
    }
    if (trimmedAk) {
      payload.access_key = trimmedAk
    }
    if (trimmedSk) {
      payload.secret_key = trimmedSk
    }
  } else {
    if (!configured && !trimmedApiKey) {
      return { error: '请输入密钥' }
    }
    if (trimmedApiKey) {
      payload.api_key = trimmedApiKey
    }
  }

  return { payload }
}

export function formatProviderStatus(item) {
  if (!item?.configured) {
    return '未配置'
  }
  return item.enabled ? '已启用' : '已停用'
}
