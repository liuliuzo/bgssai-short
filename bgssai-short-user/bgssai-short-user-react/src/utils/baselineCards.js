export function visibleBaseline(precheck) {
  const characters = (precheck?.character_version_list || []).filter(
    (row) => row && row.card_status === 'LOCKED' && row.version_label === '角色版本',
  )
  const timeline = precheck?.timeline_version
  const timelineLocked = timeline
    && timeline.card_status === 'LOCKED'
    && timeline.version_label === '时间轴版本'
    ? timeline
    : null
  const voided = (precheck?.voided_character_list || []).filter(
    (row) => row && row.voided_flag === 1 && row.consistent_flag === 0,
  )
  return { characters, timeline: timelineLocked, voided }
}

export function publishStatusAlertType(status) {
  if (status === 'FAILED') return 'error'
  if (status === 'UNKNOWN') return 'warning'
  if (status === 'PENDING' || status === 'PROCESSING' || status === 'QUEUED') return 'info'
  if (status === 'READY') return 'success'
  return 'warning'
}

export function tubeTokenMissingMessage(preflight) {
  if (!preflight) return ''
  if (preflight.token_configured === false) return '未配置 Tube 令牌'
  const text = Array.isArray(preflight.warnings) ? preflight.warnings.join(' ') : ''
  if (/未配置 Tube 令牌|ingest-token/.test(text)) return '未配置 Tube 令牌'
  return ''
}

/**
 * Short reasons beside 「发布到 Tube」 only.
 * A not-yet-composed episode has no film row; that is not shown here.
 * Token absence is not joined with other preflight text.
 */
export function composePublishHints(preflight, film) {
  const hints = []
  const tokenMissing = Boolean(tubeTokenMissingMessage(preflight))
  if (tokenMissing) hints.push('未配置 Tube 令牌')
  else if (preflight && preflight.base_url_configured === false) hints.push('Tube 主机未配置')
  if (preflight?.film_record_missing === true) {
    const reason = String(preflight.gate_reason || '').trim()
    hints.push(reason || '成片记录缺失，请重新合成')
    return hints
  }
  const hostMissing = preflight && preflight.base_url_configured === false
  if (!tokenMissing && !hostMissing && film?.film_id && preflight?.publishable === false) {
    const reason = String(preflight.gate_reason || '').trim()
    if (reason && !/film not found/i.test(reason)) hints.push(reason)
  }
  return hints
}
