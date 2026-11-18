export function formatKeyBalance(row) {
  if (!row || !row.balance_status) {
    return '-'
  }
  if (row.balance_status === 'UNAVAILABLE') {
    return '不支持查余额'
  }
  if (row.balance_status !== 'OK') {
    return '查询失败'
  }
  const list = Array.isArray(row.balance_list) ? row.balance_list : []
  const parts = list.map((part) => {
    const amount = part?.amount
    if (amount == null || String(amount).trim() === '') {
      return ''
    }
    const label = part.label ? `${part.label} ` : ''
    const unit = part.unit ? ` ${part.unit}` : ''
    return `${label}${amount}${unit}`
  }).filter(Boolean)
  return parts.length ? parts.join(' · ') : '-'
}
