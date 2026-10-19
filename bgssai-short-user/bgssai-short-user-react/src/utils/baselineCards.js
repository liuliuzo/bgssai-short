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

export function tubeTokenMissingMessage(preflight) {
  if (!preflight) return ''
  if (preflight.token_configured === false) return '未配置 Tube 令牌'
  const text = Array.isArray(preflight.warnings) ? preflight.warnings.join(' ') : ''
  if (/未配置 Tube 令牌|ingest-token/.test(text)) return '未配置 Tube 令牌'
  return ''
}
