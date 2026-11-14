import { GATE_STATUS_LABEL, labelOf } from '../../lib/displayLabels.js'

function hasNumber(value) {
  return value != null && value !== '' && Number.isFinite(Number(value))
}

function gateBody(result) {
  if (!result || typeof result !== 'object') return null
  if (result.gate_id || result.gate_status || hasNumber(result.score)) return result
  return null
}

export function qualityHeadline({ profile, gateResult, gateCode, loading = false } = {}) {
  if (loading && !profile && !gateBody(gateResult)) {
    return { kind: 'loading', text: '正在读取质检结果' }
  }
  const gate = gateBody(gateResult)
  const listed = (profile?.gate_list || []).find((row) => !gateCode || row?.gate_code === gateCode)
  const score = hasNumber(gate?.score) ? gate.score : (hasNumber(listed?.score) ? listed.score : null)
  const status = gate?.gate_status || listed?.gate_status || ''
  const threshold = hasNumber(profile?.quality_threshold_score) ? profile.quality_threshold_score : null
  const overall = hasNumber(profile?.overall_quality_score) ? profile.overall_quality_score : null
  if (threshold == null && overall == null && score == null && !status) {
    return {
      kind: 'empty',
      text: '还没有质检结果。执行质检之后，这里才会出现作品阈值、总分和当前门。',
    }
  }
  const thresholdText = threshold == null ? '作品阈值 尚未设置' : `作品阈值 ${threshold}`
  const overallText = overall == null ? '总分 尚未汇总' : `总分 ${overall}`
  const gateText = status || score != null
    ? `当前门 ${status ? labelOf(GATE_STATUS_LABEL, status, status) : '已有分数'}${score != null ? ` / ${score}` : ''}`
    : '当前门 尚未执行'
  return { kind: 'ready', text: `${thresholdText} · ${overallText} · ${gateText}` }
}
