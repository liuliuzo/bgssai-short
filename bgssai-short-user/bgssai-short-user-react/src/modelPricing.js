import { formatTokenAmount, formatTokenExact } from './utils/formatToken.js'

export const ALLOWED_MULTIPLIERS = [0.5, 1, 1.5]
export const DOMESTIC_GATEWAY_REGION = 'tokenhub-cn'

const RAW_VENDOR_HOST_MARKERS = [
  'api.openai.com',
  'openai.azure.com',
  'api.anthropic.com',
  'api.deepseek.com',
  'dashscope.aliyuncs.com',
  'open.bigmodel.cn',
  'api.moonshot.cn',
  'api.minimax.chat',
  'klingai.com',
]

const STATUS_LABEL = {
  pending: '待对账',
  settled: '已结算',
  unpaid: '未支付',
  unpriced: '未定价',
  reversed: '已冲正',
}

function fenToYuan(fen) {
  if (fen == null || fen === '' || typeof fen === 'boolean') return null
  const n = Number(fen)
  if (!Number.isFinite(n)) return null
  return (n / 100).toFixed(2)
}

export function isValidBillingMultiplier(multiplier) {
  if (multiplier == null || multiplier === '' || typeof multiplier === 'boolean') return false
  const n = Number(multiplier)
  return ALLOWED_MULTIPLIERS.some((allowed) => Math.abs(n - allowed) < 1e-9)
}

export function validateTokenhubCnRoute(row = {}) {
  const isString = typeof row === 'string'
  const region = isString
    ? DOMESTIC_GATEWAY_REGION
    : String(row?.region || row?.gateway_region || row?.provider || DOMESTIC_GATEWAY_REGION)
        .trim()
        .toLowerCase()
  const endpoint = isString
    ? row.trim().toLowerCase()
    : String(row?.endpoint || row?.base_url || '').trim().toLowerCase()

  if (region.includes('global') || region.includes('intl') || endpoint.includes('tokenhub-global') || endpoint.includes('bgssai-tokenhub.com')) {
    return {
      valid: false,
      region: DOMESTIC_GATEWAY_REGION,
      reason: '该模型不在当前可用范围，请改选其它模型',
    }
  }
  for (const marker of RAW_VENDOR_HOST_MARKERS) {
    if (endpoint.includes(marker)) {
      return {
        valid: false,
        region: DOMESTIC_GATEWAY_REGION,
        reason: '不能使用厂商直连地址，请改选目录中的模型',
      }
    }
  }
  if (!isString && row?.billing_multiplier != null && row?.billing_multiplier !== '' && !isValidBillingMultiplier(row.billing_multiplier)) {
    return {
      valid: false,
      region: DOMESTIC_GATEWAY_REGION,
      reason: `模型额度倍率须为 0.5 / 1 / 1.5 倍（当前为 ${row.billing_multiplier}）`,
    }
  }
  return {
    valid: true,
    region: DOMESTIC_GATEWAY_REGION,
    reason: '',
  }
}

export function detectMultiplierDrift(selectedModel = {}, latestCatalogRow = {}) {
  if (!selectedModel || !latestCatalogRow) {
    return { changed: false, drifted: false, valid: true, reason: '' }
  }
  const routeCheck = validateTokenhubCnRoute(latestCatalogRow)
  if (!routeCheck.valid) {
    return {
      changed: true,
      drifted: true,
      valid: false,
      reason: routeCheck.reason,
      expectedMultiplier: selectedModel.billing_multiplier ?? null,
      actualMultiplier: latestCatalogRow.billing_multiplier ?? null,
    }
  }
  const prevMult = selectedModel.billing_multiplier == null ? null : Number(selectedModel.billing_multiplier)
  const nextMult = latestCatalogRow.billing_multiplier == null ? null : Number(latestCatalogRow.billing_multiplier)
  if (prevMult != null && nextMult != null && Math.abs(prevMult - nextMult) > 1e-9) {
    return {
      changed: true,
      drifted: true,
      valid: isValidBillingMultiplier(nextMult),
      reason: `模型额度倍率已由 ${prevMult}x 变更为 ${nextMult}x，请重新确认后再提交`,
      expectedMultiplier: prevMult,
      actualMultiplier: nextMult,
    }
  }
  if (
    selectedModel.price_version != null &&
    latestCatalogRow.price_version != null &&
    String(selectedModel.price_version) !== String(latestCatalogRow.price_version)
  ) {
    return {
      changed: true,
      drifted: true,
      valid: true,
      reason: `模型计价版本已由 v${selectedModel.price_version} 更新为 v${latestCatalogRow.price_version}，请重新确认后再提交`,
      expectedMultiplier: prevMult,
      actualMultiplier: nextMult,
    }
  }
  return {
    changed: false,
    drifted: false,
    valid: nextMult == null ? true : isValidBillingMultiplier(nextMult),
    reason: '',
    expectedMultiplier: prevMult,
    actualMultiplier: nextMult,
  }
}

export function estimateShotBatchTokens(shotCount, modelRow = {}, baseTokensPerShot = 100) {
  const count = Array.isArray(shotCount)
    ? shotCount.length
    : Math.max(0, Math.trunc(Number(shotCount) || 0))
  const isModelNumber = typeof modelRow === 'number'
  const base = Math.max(0, Math.trunc(Number(isModelNumber ? modelRow : baseTokensPerShot) || 0))
  const rawTokens = count * base
  const rawMult = isModelNumber
    ? (typeof baseTokensPerShot === 'number' ? baseTokensPerShot : 1)
    : (modelRow?.billing_multiplier == null ? 1 : Number(modelRow.billing_multiplier))
  const validMultiplier = isValidBillingMultiplier(rawMult)
  const effectiveMultiplier = validMultiplier ? rawMult : 1
  const billedTokens = Math.round(rawTokens * effectiveMultiplier)
  return {
    shotCount: count,
    shot_count: count,
    rawTokens,
    raw_tokens: rawTokens,
    billedTokens,
    billed_tokens: billedTokens,
    multiplier: validMultiplier ? rawMult : null,
    validMultiplier,
    compactBilled: formatTokenAmount(billedTokens),
    exactBilled: formatTokenExact(billedTokens),
  }
}

function hideGatewayBrand(text) {
  return String(text || '').replace(/token\s*-?\s*hub/ig, '').replace(/\s{2,}/g, ' ').trim()
}

export function modelPricingLabel(row = {}) {
  const name = hideGatewayBrand(row.label || row.display_name || row.model_name || row.model_code || row.model || '')
  const multiplier = row.billing_multiplier
  const value = multiplier == null ? null : Number(multiplier)
  const parts = [name]
  if (isValidBillingMultiplier(value)) {
    parts.push(`${value}×`)
  }
  if (row.price_version != null) {
    parts.push(`v${row.price_version}`)
  }
  const unit = (row.price_unit || '').toLowerCase()
  const currency = row.currency || 'CNY'
  if (unit === 'token' && row.input_fen_per_million != null) {
    const inYuan = fenToYuan(row.input_fen_per_million)
    const outYuan = fenToYuan(row.output_fen_per_million)
    parts.push(`输入/输出 ${inYuan ?? '-'}/${outYuan ?? '-'} 元/百万额度`)
  } else if (unit === 'image' && row.image_fen_per_unit != null) {
    parts.push(`${fenToYuan(row.image_fen_per_unit)} 元/张`)
  } else if (['audio_second', 'audio'].includes(unit) && row.audio_fen_per_second != null) {
    parts.push(`${fenToYuan(row.audio_fen_per_second)} 元/秒`)
  } else if (['video_second', 'video'].includes(unit) && row.video_fen_per_second != null) {
    parts.push(`${fenToYuan(row.video_fen_per_second)} 元/秒`)
  } else if (parts.length === 1) {
    parts.push('按任务实际对账')
  }
  return parts.filter(Boolean).join(' · ')
}

export function receiptSummary(receipt = {}) {
  const status = STATUS_LABEL[receipt.billing_status] || (receipt.billing_status ? String(receipt.billing_status) : '待对账')
  const billed = receipt.billed_tokens == null ? '待对账' : String(receipt.billed_tokens)
  const unit = receipt.billing_unit || '额度'
  const amountYuan = fenToYuan(receipt.debit_fen)
  const amount = amountYuan == null ? '待对账' : `${amountYuan} 元`
  const currency = receipt.currency && receipt.currency !== 'CNY' ? `（${receipt.currency}）` : ''
  const multiplier = isValidBillingMultiplier(receipt.billing_multiplier) ? `${Number(receipt.billing_multiplier)}× · ` : ''
  return `${status} · ${multiplier}实扣 ${billed} ${unit} · 金额 ${amount}${currency}`
}

