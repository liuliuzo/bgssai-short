import test from 'node:test'
import assert from 'node:assert/strict'
import { groupLooks, groupLooksOfKind, lookCover, referenceLine } from './lookGroups.js'

test('同一造型的四面归到一张卡上', () => {
  const groups = groupLooks([
    { material_name: '白纱花瓣', view_angle: 'SIDE' },
    { material_name: '白衬衫', view_angle: 'FRONT_FACE' },
    { material_name: '白纱花瓣', view_angle: 'FRONT_FACE' },
    { material_name: '白纱花瓣', view_angle: 'SIDE' },
  ])
  assert.deepEqual(groups.map((group) => group.name), ['白纱花瓣', '白衬衫'])
  assert.equal(groups[0].slots.SIDE.length, 2)
  assert.equal(groups[0].slots.FRONT_FACE.length, 1)
  assert.equal(groups[0].slots.BACK.length, 0)
  assert.equal(groups[1].slots.FRONT_FACE.length, 1)
})

test('只拿出要的那一类，封面优先正脸', () => {
  const groups = groupLooksOfKind([
    { material_name: '天台', material_kind: 'SCENE', view_angle: null, file_url: 'scene' },
    { material_name: '白纱', material_kind: 'CHARACTER', view_angle: 'SIDE', file_url: 'side' },
    { material_name: '白纱', material_kind: 'CHARACTER', view_angle: 'FRONT_FACE', file_url: 'face' },
  ], 'CHARACTER')
  assert.equal(groups.length, 1)
  assert.equal(lookCover(groups[0]).file_url, 'face')
})

test('生成前的说明写清用哪张，或写清不用图', () => {
  assert.equal(referenceLine(null), '这次没有参考图，只按外观描述生成。')
  assert.equal(referenceLine({ used: 1, source: 'CARD', library_name: '我的定妆', material_name: '沈知微', view_name: '正脸' }),
    '生成时会用「我的定妆」里的「沈知微 · 正脸」。')
  assert.match(referenceLine({ used: 1, source: 'NAME', library_name: '礼服装', material_name: '红礼服' }), /比较接近/)
  assert.equal(referenceLine({ used: 1, source: 'CARD', library_name: '我的定妆', material_name: '沈知微' }, true),
    '这次不使用参考图，只按外观描述生成。')
})

test('没有角度的素材单独放着', () => {
  const groups = groupLooks([{ material_name: '天台', view_angle: null }])
  assert.equal(groups[0].extra.length, 1)
  assert.equal(groups[0].slots.FRONT.length, 0)
})
