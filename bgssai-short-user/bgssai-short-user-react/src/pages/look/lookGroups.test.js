import test from 'node:test'
import assert from 'node:assert/strict'
import { groupLooks } from './lookGroups.js'

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

test('没有角度的素材单独放着', () => {
  const groups = groupLooks([{ material_name: '天台', view_angle: null }])
  assert.equal(groups[0].extra.length, 1)
  assert.equal(groups[0].slots.FRONT.length, 0)
})
