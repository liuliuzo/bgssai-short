import { COMPOSE_STATUS_LABEL, RISK_LEVEL_LABEL, labelOf } from '../lib/displayLabels.js'

const READY_PASS_WORDS = ['已就绪', '已通过', '可合成', 'READY', 'PASS', 'APPROVED']

export function claimsReadyPass(text) {
  const value = String(text || '')
  return READY_PASS_WORDS.some((word) => value.includes(word))
}

function riskLabel(precheck) {
  const risk = precheck?.risk_level
  if (risk == null || risk === '') return '-'
  return labelOf(RISK_LEVEL_LABEL, risk, String(risk))
}

function composeLabel(code) {
  if (code == null || code === '' || code === '-') return '-'
  return labelOf(COMPOSE_STATUS_LABEL, code, String(code))
}

/**
 * 成片页状态口径。
 * 剧集列表上的剧本「已就绪」/ 审核「已通过」以及较粗的 compose_status=READY
 * 不能在预检尚未返回时当作合成结论。未返回时两边都显示核对中，不把空预检画成「未就绪」。
 */
export function composeSurface({ settled = false, error = false, film = null, precheck = null } = {}) {
  if (!settled) {
    return {
      phase: 'loading',
      paneText: '合成状态 加载中 / 预检 核对中 / 风险 -',
      chipText: '核对中',
      allowReadyPassChip: false,
    }
  }
  if (error) {
    return {
      phase: 'error',
      paneText: '合成状态 未能核对 / 预检 未能核对 / 风险 -',
      chipText: '未能核对',
      allowReadyPassChip: false,
    }
  }
  const compose = film?.compose_status || '-'
  const preflightReady = Boolean(precheck?.ready)
  const riskCode = precheck?.risk_level
  const risk = riskLabel(precheck)
  const expired = film?.expired_flag === 1 ? ' / 成片已过期' : ''
  const riskOk = riskCode == null || riskCode === '' || riskCode === 'PASS'
  const affirmative = preflightReady && compose === 'READY' && riskOk
  let chipText = '未就绪'
  if (affirmative) chipText = '已就绪'
  else if (preflightReady) chipText = '可合成'
  return {
    phase: 'settled',
    paneText: `合成状态 ${composeLabel(compose)} / 预检 ${preflightReady ? '可合成' : '未就绪'} / 风险 ${risk}${expired}`,
    chipText,
    allowReadyPassChip: affirmative,
    preflightReady,
    compose,
    risk,
  }
}

export function composeSurfaceFromEntry(entry) {
  if (!entry || entry.phase === 'loading') return composeSurface({ settled: false })
  if (entry.phase === 'error') return composeSurface({ settled: true, error: true })
  return composeSurface({ settled: true, film: entry.film, precheck: entry.precheck })
}

function scriptReviewPair(ep) {
  return `${ep?.script_status_name || ep?.script_status || '-'}/${ep?.review_status_name || ep?.review_status || '-'}`
}

export function episodeSelectLabel(ep, { onCompose = false, surface = null } = {}) {
  const head = `第${ep?.episode_no ?? ''}集 ${ep?.episode_title || ''}`.trim()
  const stored = scriptReviewPair(ep)
  if (!onCompose) return `${head}（${stored}）`
  if (surface?.allowReadyPassChip) return `${head}（${stored}）`
  return `${head}（${surface?.chipText || '核对中'}）`
}

export function episodeComposeTag(ep, { onCompose = false, surface = null } = {}) {
  if (!onCompose) return scriptReviewPair(ep)
  if (surface?.allowReadyPassChip) return '已就绪'
  return surface?.chipText || '核对中'
}

export function episodeComposeTagColor(onCompose, surface) {
  if (!onCompose || !surface) return undefined
  if (surface.phase === 'loading') return 'processing'
  if (surface.phase === 'error') return 'default'
  if (!surface.allowReadyPassChip) return 'warning'
  return 'success'
}
