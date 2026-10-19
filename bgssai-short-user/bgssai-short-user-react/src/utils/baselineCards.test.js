import test from 'node:test'
import assert from 'node:assert/strict'
import { tubeTokenMissingMessage, visibleBaseline } from './baselineCards.js'

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

test('missing tube token uses the disabled-state copy', () => {
  assert.equal(tubeTokenMissingMessage({ token_configured: false, config_ready: false }), '未配置 Tube 令牌')
  assert.equal(tubeTokenMissingMessage({ warnings: ['未配置 bgssai.tube.ingest-token'] }), '未配置 Tube 令牌')
  assert.equal(tubeTokenMissingMessage({ token_configured: true, config_ready: true }), '')
})
