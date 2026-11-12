import test from 'node:test'
import assert from 'node:assert/strict'
import { composePublishHints, publishStatusAlertType, tubeTokenMissingMessage, visibleBaseline } from './baselineCards.js'

test('locked cards keep Chinese version labels and voided cards stay inconsistent', () => {
  const view = visibleBaseline({
    character_version_list: [
      { character_id: 'c1', card_status: 'LOCKED', version_label: '角色版本', version_text: '沈知微 · 已锁定定妆' },
      { character_id: 'c2', card_status: 'DRAFT', version_label: '角色版本', version_text: '不应出现' },
    ],
    timeline_version: {
      card_status: 'LOCKED',
      version_label: '时间轴版本',
      version_text: 'v2 · 1 镜 · 总时长 4 秒 · 字幕 1 条',
    },
    voided_character_list: [
      { character_name: '陆时衍', voided_flag: 1, consistent_flag: 0, version_text: '陆时衍 已作废，不能标为与现行时间轴一致' },
    ],
  })
  assert.equal(view.characters.length, 1)
  assert.equal(view.characters[0].version_label, '角色版本')
  assert.equal(view.timeline.version_label, '时间轴版本')
  assert.equal(view.voided.length, 1)
  assert.equal(view.voided[0].consistent_flag, 0)
})

test('unknown publish status is visible and is not a success', () => {
  assert.equal(publishStatusAlertType('UNKNOWN'), 'warning')
  assert.equal(publishStatusAlertType('PENDING'), 'info')
  assert.equal(publishStatusAlertType('READY'), 'success')
  assert.notEqual(publishStatusAlertType('UNKNOWN'), 'success')
})

test('missing tube token uses the disabled-state copy', () => {
  assert.equal(tubeTokenMissingMessage({ token_configured: false, config_ready: false }), '未配置 Tube 令牌')
  assert.equal(tubeTokenMissingMessage({ warnings: ['未配置 bgssai.tube.ingest-token'] }), '未配置 Tube 令牌')
  assert.equal(tubeTokenMissingMessage({ token_configured: true, config_ready: true }), '')
})

test('composable episode does not mix token absence with a missing film row', () => {
  const preflight = {
    token_configured: false,
    config_ready: false,
    base_url_configured: true,
    film_present: false,
    film_record_missing: false,
    publishable: false,
    gate_reason: '本集尚未合成，合成完成后再发布',
    warnings: [
      '未配置 Tube 令牌：请在环境配置或管理端写入摄入令牌后再发布（勿提交真实密钥，不要编造令牌）',
    ],
  }
  const film = { episode_id: 'ep-1', compose_status: 'NONE' }
  assert.deepEqual(composePublishHints(preflight, film), ['未配置 Tube 令牌'])
  assert.equal(composePublishHints(preflight, film).join('；').includes('film not found'), false)
  assert.equal(composePublishHints(preflight, film).join('').includes('尚未合成'), false)
})

test('missing film record is a separate publish hint from the token', () => {
  const hints = composePublishHints({
    token_configured: false,
    base_url_configured: true,
    film_record_missing: true,
    gate_reason: '成片记录缺失，请重新合成后再发布',
  }, { compose_status: 'READY' })
  assert.deepEqual(hints, ['未配置 Tube 令牌', '成片记录缺失，请重新合成后再发布'])
})

test('an existing film that cannot be published keeps its own gate beside the button', () => {
  const hints = composePublishHints({
    token_configured: true,
    base_url_configured: true,
    config_ready: true,
    publishable: false,
    film_record_missing: false,
    gate_reason: '剧集审核未通过（review_status=NOT_SUBMITTED，需 APPROVED）后才能发布到 Tube',
  }, { film_id: 'f-1', compose_status: 'READY' })
  assert.equal(hints.length, 1)
  assert.match(hints[0], /审核未通过/)
})
