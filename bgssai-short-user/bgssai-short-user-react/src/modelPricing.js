export function modelPricingLabel(row = {}) {
  const name = row.label || row.display_name || row.model_name || row.model_code || row.model || ''
  const multiplier = row.billing_multiplier
  const value = multiplier == null ? null : Number(multiplier)
  const rate = value != null && Number.isFinite(value) && value >= 0 ? `${value}× / ${row.billing_unit || '单位待确认'}` : '倍率待确认'
  const version = row.price_version == null ? '价格版本待确认' : `v${row.price_version}`
  const currency = row.currency || '币种待确认'
  const unit = (row.price_unit || '').toLowerCase()
  const quote = unit === 'token' && row.input_fen_per_million != null
    ? `输入/输出 ${row.input_fen_per_million}/${row.output_fen_per_million ?? '待确认'} ${currency} 分/百万 token`
    : unit === 'image' && row.image_fen_per_unit != null ? `${row.image_fen_per_unit} ${currency} 分/张`
    : ['audio_second', 'audio'].includes(unit) && row.audio_fen_per_second != null ? `${row.audio_fen_per_second} ${currency} 分/秒`
    : ['video_second', 'video'].includes(unit) && row.video_fen_per_second != null ? `${row.video_fen_per_second} ${currency} 分/秒`
    : `金额按任务实际对账 / ${row.price_unit || '计价单位待确认'}`
  return `${name} · ${rate} · ${version} · ${quote}`
}
export function receiptSummary(receipt = {}) {
  const value = (v) => v == null ? '待对账' : String(v)
  return `${receipt.billing_status || 'pending'} · 原始 ${value(receipt.raw_tokens)} · 实扣 ${value(receipt.billed_tokens)} ${receipt.billing_unit || 'token'} · 倍率 ${value(receipt.billing_multiplier)} · 价格版本 ${value(receipt.price_version)} · 金额 ${value(receipt.debit_fen)} ${receipt.currency || '币种待确认'} 分`
}
