import test from 'node:test'
import assert from 'node:assert/strict'
import {
  labelOf,
  ROLE_LABEL,
  SCRIPT_STATUS_LABEL,
  GATE_STATUS_LABEL,
  RELATION_TYPE_LABEL,
  CONTINUITY_TYPE_LABEL,
  SEVERITY_LABEL,
  HANDLE_STATUS_LABEL,
  FORESHADOW_STATUS_LABEL,
  PREVIZ_LEVEL_LABEL,
  TARGET_TYPE_LABEL,
  PLAN_CODE_LABEL,
  SUB_STATUS_LABEL,
  ORDER_TYPE_LABEL,
  ORDER_STATUS_LABEL,
  LOGIN_METHOD_LABEL,
  COMPOSE_STATUS_LABEL,
  RISK_LEVEL_LABEL,
  draftFlagLabel,
  softenTechId,
  localizePromptShotSize,
  friendlyFailReason,
  visibleFailReason,
  sanitizeTubeWarning,
  roleBadgeLabel,
  planDisplayName,
} from './displayLabels.js'

test('role badge USER → 用户', () => {
  assert.equal(roleBadgeLabel(false), '用户')
  assert.equal(ROLE_LABEL.USER, '用户')
})

test('studio status enums map to Chinese', () => {
  assert.equal(labelOf(SCRIPT_STATUS_LABEL, 'READY'), '已就绪')
  assert.equal(labelOf(GATE_STATUS_LABEL, 'PASSED'), '已通过')
  assert.equal(labelOf(COMPOSE_STATUS_LABEL, 'READY'), '已就绪')
  assert.equal(labelOf(RISK_LEVEL_LABEL, 'PASS'), '通过')
  assert.equal(labelOf(PREVIZ_LEVEL_LABEL, 'TEXT'), '文字')
  assert.equal(labelOf(TARGET_TYPE_LABEL, 'PORTRAIT'), '定妆')
  assert.equal(labelOf(TARGET_TYPE_LABEL, 'COMPOSE'), '成片合成')
  assert.equal(labelOf(TARGET_TYPE_LABEL, 'SCRIPT'), '剧本')
  assert.equal(labelOf(TARGET_TYPE_LABEL, 'SHOT'), '分镜')
})

test('story-map and continuity enums', () => {
  assert.equal(draftFlagLabel(0), '正式')
  assert.equal(draftFlagLabel(1), '草稿估算')
  assert.equal(labelOf(RELATION_TYPE_LABEL, 'MENTOR'), '师徒')
  assert.equal(labelOf(RELATION_TYPE_LABEL, 'RIVAL'), '对立')
  assert.equal(labelOf(RELATION_TYPE_LABEL, 'RELATED'), '关联')
  assert.equal(labelOf(CONTINUITY_TYPE_LABEL, 'SPACE'), '空间')
  assert.equal(labelOf(CONTINUITY_TYPE_LABEL, 'EYELINE'), '视线')
  assert.equal(labelOf(SEVERITY_LABEL, 'MINOR'), '轻微')
  assert.equal(labelOf(HANDLE_STATUS_LABEL, 'RESOLVED'), '已处理')
  assert.equal(labelOf(HANDLE_STATUS_LABEL, 'OPEN'), '待处理')
  assert.equal(labelOf(FORESHADOW_STATUS_LABEL, 'CLOSED'), '已回收')
  assert.equal(labelOf(FORESHADOW_STATUS_LABEL, 'OPEN'), '未回收')
})

test('softens technical ids for primary copy', () => {
  assert.equal(softenTechId('ch-dm-005-01', 'character'), '角色 1')
  assert.match(softenTechId('sh-dm-005-01-10', 'shot'), /镜/)
  assert.equal(softenTechId('as-dm-005-01', 'asset'), '素材')
})

test('localizes trailing shot-size tokens in prompts', () => {
  assert.equal(localizePromptShotSize('cold light, medium'), 'cold light，中景')
  assert.equal(localizePromptShotSize('face, close'), 'face，特写')
  assert.equal(localizePromptShotSize('room, wide'), 'room，全景')
})

test('success or empty fail code leaves the failure column blank', () => {
  assert.equal(visibleFailReason({
    task_status: 'SUCCEEDED',
    task_status_name: '已成功',
    fail_reason_code: null,
    fail_reason: '模型暂时不可用，请稍后重试或更换模型',
  }), '')
  assert.equal(visibleFailReason({
    task_status: 'SUCCESS',
    fail_reason_code: 'MODEL_TIMEOUT',
    fail_reason: '模型超时',
  }), '')
  assert.equal(visibleFailReason({
    task_status: 'FAILED',
    fail_reason_code: '',
    fail_reason: '模型暂时不可用，请稍后重试或更换模型',
  }), '')
  assert.equal(visibleFailReason({
    task_status: 'RUNNING',
    fail_reason_code: null,
    fail_reason: null,
  }), '')
  assert.equal(visibleFailReason({
    task_status: 'FAILED',
    fail_reason_code: 'CONTENT_BLOCKED',
    fail_reason: '内容命中拦截词',
  }), '内容命中拦截词')
})

test('friendly fail reason scrubs TLS English', () => {
  assert.equal(
    friendlyFailReason('Remote host terminated the handshake'),
    '暂时无法拉取任务：连接失败，请稍后重试',
  )
  assert.equal(friendlyFailReason('内容命中拦截词'), '内容命中拦截词')
  assert.equal(friendlyFailReason('Tube 响应缺少 media_id'), 'Tube 响应缺少 作品号')
  assert.doesNotMatch(friendlyFailReason('tokenhub gateway down'), /tokenhub/i)
  assert.doesNotMatch(friendlyFailReason('模型服务暂未开通：deepseek，请稍后重试'), /deepseek/i)
  assert.match(friendlyFailReason('模型服务暂未开通：deepseek，请稍后重试'), /模型服务暂未开通/)
})

test('sanitizeTubeWarning hides ingest-token key', () => {
  const out = sanitizeTubeWarning('未配置 Tube 令牌：请在环境配置或管理端写入 bgssai.tube.ingest-token（勿提交真实密钥）')
  assert.equal(out.includes('ingest-token'), false)
  assert.equal(out.includes('bgssai.tube'), false)
  assert.match(out, /Tube 令牌/)
})

test('sanitizeTubeWarning drops film-not-found so it cannot be joined into the token hint', () => {
  assert.equal(sanitizeTubeWarning('film not found for episode'), '')
  const joined = ['未配置 Tube 令牌：请在环境配置或管理端写入令牌后再发布', 'film not found for episode']
    .map(sanitizeTubeWarning)
    .filter(Boolean)
    .join('；')
  assert.equal(joined.includes('film not found'), false)
  assert.match(joined, /Tube 令牌/)
})

test('subscription and account enums', () => {
  assert.equal(labelOf(PLAN_CODE_LABEL, 'FREE'), '免费')
  assert.equal(labelOf(PLAN_CODE_LABEL, 'PRO'), '专业')
  assert.equal(labelOf(PLAN_CODE_LABEL, 'MAX5'), 'MAX 5')
  assert.equal(labelOf(SUB_STATUS_LABEL, 'active'), '生效中')
  assert.equal(labelOf(ORDER_TYPE_LABEL, 'subscription'), '订阅套餐')
  assert.equal(labelOf(ORDER_TYPE_LABEL, 'credit_pack'), '加量包')
  assert.equal(labelOf(ORDER_STATUS_LABEL, 'paid'), '已支付')
  assert.equal(labelOf(LOGIN_METHOD_LABEL, 'password'), '密码登录')
  assert.equal(planDisplayName('PRO', '专业'), '专业')
  assert.equal(planDisplayName('PRO', null), '专业')
})
