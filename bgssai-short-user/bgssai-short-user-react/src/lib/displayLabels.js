/** User-facing Chinese labels. Internal API enums stay English. */

import { scrubVendorText } from '../publicModel.js'

export const ROLE_LABEL = {
  USER: '用户',
  BOT: 'BOT',
  PLATFORM_ADMIN: '平台管理员',
}

export const SCRIPT_STATUS_LABEL = {
  NONE: '未开始',
  DRAFT: '草稿',
  READY: '已就绪',
  GENERATING: '生成中',
  FAILED: '失败',
  LOCKED: '已锁定',
}

export const SHOT_STATUS_LABEL = {
  PENDING: '待处理',
  DRAFT: '草稿',
  TEXT_READY: '文字稿',
  DRAFT_READY: '草图',
  IMAGE_GENERATING: '出图中',
  IMAGE_READY: '首帧',
  VIDEO_GENERATING: '出视频',
  VIDEO_READY: '视频',
  PROMPT_READY: '提示词就绪',
  LOCKED: '已锁定',
  FAILED: '失败',
}

export const GATE_STATUS_LABEL = {
  PASSED: '已通过',
  FAILED: '未通过',
  PENDING: '待跑',
  SKIPPED: '已跳过',
  FORCE_PASSED: '强制放行',
}

export const COMPOSE_STATUS_LABEL = {
  NONE: '未合成',
  PENDING: '合成中',
  READY: '已就绪',
  FAILED: '失败',
  EXPIRED: '已过期',
}

export const RISK_LEVEL_LABEL = {
  PASS: '通过',
  WARN: '提醒',
  FAIL: '未通过',
  BLOCK: '拦截',
}

export const RELATION_TYPE_LABEL = {
  MENTOR: '师徒',
  RIVAL: '对立',
  RELATED: '关联',
  ALLY: '同盟',
  FAMILY: '亲属',
  LOVE: '情感',
  ENEMY: '敌对',
}

export const CONTINUITY_TYPE_LABEL = {
  SPACE: '空间',
  EYELINE: '视线',
  PROP: '道具',
  COSTUME: '服装',
  TIME: '时间',
  ACTION: '动作',
  LIGHTING: '光线',
  OTHER: '其他',
}

export const SEVERITY_LABEL = {
  MINOR: '轻微',
  MAJOR: '严重',
  CRITICAL: '致命',
}

export const HANDLE_STATUS_LABEL = {
  OPEN: '待处理',
  RESOLVED: '已处理',
  CLOSED: '已关闭',
  FIXED: '已修复',
  WONT_FIX: '不改',
  IGNORED: '已忽略',
}

export const FORESHADOW_STATUS_LABEL = {
  OPEN: '未回收',
  CLOSED: '已回收',
  DROPPED: '已放弃',
}

export const PREVIZ_LEVEL_LABEL = {
  NONE: '未预演',
  TEXT: '文字',
  DRAFT_IMAGE: '草图',
  IMAGE: '首帧',
  VIDEO: '视频',
}

export const TARGET_TYPE_LABEL = {
  OUTLINE: '大纲',
  SCRIPT: '剧本',
  ADAPT_OUTLINE: '改编大纲',
  REWRITE: '改写',
  PORTRAIT: '定妆',
  SCENE_PORTRAIT: '场景卡',
  SHOT: '分镜',
  LAST_FRAME: '尾帧',
  COMPOSE: '成片合成',
  DUBBING: '配音',
  SUBTITLE_TRANSCRIBE: '字幕识别',
  MUSIC: '音乐',
  BGM: '背景音乐',
}

export const PLAN_CODE_LABEL = {
  FREE: '免费',
  PRO: '专业',
  MAX5: 'MAX 5',
  MAX10: 'MAX 10',
  MAX20: 'MAX 20',
}

export const SUB_STATUS_LABEL = {
  active: '生效中',
  canceled: '已取消',
  expired: '已过期',
  past_due: '待续费',
  trialing: '试用中',
}

export const ORDER_TYPE_LABEL = {
  subscription: '订阅套餐',
  credit_pack: '加量包',
  paid: '已支付',
}

export const ORDER_STATUS_LABEL = {
  created: '待支付',
  paid: '已支付',
  closed: '已关闭',
  refunded: '已退款',
}

export const LOGIN_METHOD_LABEL = {
  password: '密码登录',
  phone: '手机',
  email: '邮箱',
  wechat: '微信',
  douyin: '抖音',
  baidu: '百度',
  alipay: '支付宝',
  chat: 'Chat',
  oauth: '第三方',
}

export const SHOT_SIZE_TOKEN_LABEL = {
  extreme_wide: '大远景',
  wide: '全景',
  medium: '中景',
  close: '特写',
  extreme_close: '大特写',
  close_up: '特写',
  medium_close: '中近景',
}

export function labelOf(map, code, fallback) {
  if (code == null || code === '') return fallback ?? '—'
  const key = String(code)
  return map[key] ?? map[key.toUpperCase()] ?? map[key.toLowerCase()] ?? fallback ?? key
}

export function roleBadgeLabel(inShell) {
  return inShell ? ROLE_LABEL.BOT : ROLE_LABEL.USER
}

/** Soften technical ids like ch-dm-005-01 / sh-dm-005-01-10 for primary copy. */
export function softenTechId(id, kind) {
  if (id == null || id === '') return '—'
  const s = String(id)
  if (kind === 'character' || /^ch[-_]/.test(s)) {
    const m = s.match(/(\d+)\s*$/)
    return m ? `角色 ${Number(m[1])}` : '角色'
  }
  if (kind === 'shot' || /^sh[-_]/.test(s)) {
    const parts = s.match(/(\d+)[-_](\d+)\s*$/)
    if (parts) return `镜 ${Number(parts[2])}`
    const m = s.match(/(\d+)\s*$/)
    return m ? `镜 ${Number(m[1])}` : '镜头'
  }
  if (kind === 'asset' || /^as[-_]/.test(s)) {
    return '素材'
  }
  return s
}

export function draftFlagLabel(flag) {
  if (flag === 1 || flag === '1' || flag === true) return '草稿估算'
  if (flag === 0 || flag === '0' || flag === false) return '正式'
  return '—'
}

/** Map trailing English shot-size tokens in visible prompts. */
export function localizePromptShotSize(text) {
  if (text == null || text === '') return text
  return String(text).replace(
    /(?:,\s*|\s+)(extreme[_\s-]?wide|extreme[_\s-]?close(?:[_\s-]?up)?|medium[_\s-]?close|close[_\s-]?up|wide|medium|close)\s*$/i,
    (_, token) => {
      const key = String(token).toLowerCase().replace(/[\s-]+/g, '_')
      const zh = SHOT_SIZE_TOKEN_LABEL[key] || SHOT_SIZE_TOKEN_LABEL[key.replace(/_up$/, '')]
      return zh ? `，${zh}` : `, ${token}`
    },
  )
}

/**
 * Failure column copy. Success, or an empty fail code, stays blank even when a stale
 * sentence is still on the row.
 */
export function visibleFailReason(row) {
  if (row == null || typeof row !== 'object') return ''
  const status = String(row.task_status || '').trim().toUpperCase()
  const statusName = String(row.task_status_name || '').trim()
  if (status === 'SUCCEEDED' || status === 'SUCCESS' || statusName === '已成功' || statusName === '成功') {
    return ''
  }
  const code = row.fail_reason_code
  if (code == null || String(code).trim() === '') return ''
  return friendlyFailReason(row.fail_reason)
}

/**
 * Scrub raw TLS / gateway English from historical fail_reason cells.
 * New failures should already be Chinese from the backend scrubber.
 */
export function friendlyFailReason(raw) {
  if (raw == null || raw === '') return ''
  const msg = String(raw).trim()
  const lower = msg.toLowerCase()
  if (
    lower.includes('remote host terminated the handshake')
    || lower.includes('connection reset')
    || lower.includes('unexpected eof')
    || lower.includes('sslhandshake')
    || lower.includes('handshake_failure')
    || lower.includes('handshake')
  ) {
    return '暂时无法拉取任务：连接失败，请稍后重试'
  }
  if (/bgssai\.tube\.ingest-token|ingest-token|base-url/i.test(msg)) {
    return 'Tube 发布配置未就绪，请联系管理员'
  }
  if (/token\s*-?\s*hub/i.test(msg)) {
    return '模型暂时不可用，请稍后重试或更换模型'
  }
  return scrubVendorText(msg.replace(/media_id/gi, '作品号'))
}

/** Mask config-key leaks in Tube preflight warnings shown to users. */
export function sanitizeTubeWarning(text) {
  if (text == null || text === '') return ''
  let s = String(text)
  if (/ingest-token|bgssai\.tube\.ingest-token/i.test(s)) {
    return '未配置 Tube 令牌：请在环境配置或管理端写入摄入令牌后再发布（勿提交真实密钥，不要编造令牌）'
  }
  if (/bgssai\.tube\.base-url|base-url/i.test(s)) {
    return 'Tube 主机未配置：发布会失败。请联系管理员完成接入后再发布'
  }
  return s
}

export function planDisplayName(planCode, planName) {
  if (planName) return planName
  return labelOf(PLAN_CODE_LABEL, planCode, planCode || '—')
}
