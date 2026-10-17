const STATUS_LABEL = {
  pending: '待对账',
  settled: '已结算',
  unpaid: '未支付',
  unpriced: '未定价',
  reversed: '已冲正',
}

function fenToYuan(fen) {
  if (fen == null || fen === '') return null
  const n = Number(fen)
  if (!Number.isFinite(n)) return null
  return (n / 100).toFixed(2)
}

export function modelPricingLabel(row = {}) {
  const name = row.label || row.display_name || row.model_name || row.model_code || row.model || ''
  const multiplier = row.billing_multiplier
  const value = multiplier == null ? null : Number(multiplier)
  const parts = [name]
  if (value != null && Number.isFinite(value) && value >= 0) {
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
    parts.push(`输入/输出 ${inYuan ?? '-'}/${outYuan ?? '-'} 元/百万 token`)
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
  const unit = receipt.billing_unit || 'token'
  const amountYuan = fenToYuan(receipt.debit_fen)
  const amount = amountYuan == null ? '待对账' : `${amountYuan} 元`
  const currency = receipt.currency && receipt.currency !== 'CNY' ? `（${receipt.currency}）` : ''
  return `${status} · 实扣 ${billed} ${unit} · 金额 ${amount}${currency}`
}
