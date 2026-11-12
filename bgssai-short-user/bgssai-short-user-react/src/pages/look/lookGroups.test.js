import test from 'node:test'
import assert from 'node:assert/strict'
import { arrangeAngle, groupLooks, groupLooksOfKind, lookCover, referenceLine } from './lookGroups.js'
import { laplacianScore, posterTimes } from './lookPoster.js'

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

test('参考视频不占主图，同一面的后几张照片还在', () => {
  const view = arrangeAngle([
    { material_id: 'face', mime_type: 'image/jpeg', file_url: 'face', width: 1440, height: 2160, file_size_byte: 595430 },
    { material_id: 'clip', mime_type: 'video/mp4', file_url: 'clip', poster_url: 'blur', width: 720, height: 1280, file_size_byte: 2624942 },
    { material_id: 'close', mime_type: 'image/jpeg', file_url: 'close', width: 1440, height: 2160, file_size_byte: 398620 },
  ])
  assert.equal(view.primary.material_id, 'face')
  assert.deepEqual(view.extras.map((item) => item.material_id), ['close'])
  assert.deepEqual(view.videos.map((item) => item.material_id), ['clip'])
})

test('同一张照片只显示一份，移除时能一起找到重复条目', () => {
  const view = arrangeAngle([
    { material_id: 'a', mime_type: 'image/jpeg', file_url: 'one', width: 864, height: 1152, file_size_byte: 169424 },
    { material_id: 'b', mime_type: 'image/jpeg', file_url: 'copy', width: 864, height: 1152, file_size_byte: 169424 },
  ])
  assert.equal(view.primary.material_id, 'a')
  assert.equal(view.extras.length, 0)
  assert.deepEqual(view.primary.same_material_ids, ['a', 'b'])
})

test('尺寸不全的照片不合并，避免误伤不同的图', () => {
  const view = arrangeAngle([
    { material_id: 'a', mime_type: 'image/jpeg', file_url: 'one' },
    { material_id: 'b', mime_type: 'image/jpeg', file_url: 'two' },
  ])
  assert.equal(view.primary.material_id, 'a')
  assert.equal(view.extras.length, 1)
})

test('封面优先用照片，不用视频那一帧', () => {
  const groups = groupLooks([
    { material_name: '红礼服', view_angle: 'FRONT_FACE', mime_type: 'video/mp4', file_url: 'clip', poster_url: 'blur' },
    { material_name: '红礼服', view_angle: 'SIDE', mime_type: 'image/jpeg', file_url: 'side' },
    { material_name: '红礼服', view_angle: 'FRONT_FACE', mime_type: 'image/jpeg', file_url: 'face' },
  ])
  assert.equal(lookCover(groups[0]).file_url, 'face')
})

test('视频封面躲开开头的晃动', () => {
  assert.deepEqual(posterTimes(0), [0])
  assert.deepEqual(posterTimes(3), [0.12, 0.45, 0.9, 1.6, 2.4])
  const flat = solid(12, 12, 30)
  const edge = solid(12, 12, 30)
  for (let y = 0; y < 12; y += 1) {
    for (let x = 6; x < 12; x += 1) {
      const i = (y * 12 + x) * 4
      edge.data[i] = 220
      edge.data[i + 1] = 220
      edge.data[i + 2] = 220
    }
  }
  assert.ok(laplacianScore(edge) > laplacianScore(flat) * 5)
})

function solid(width, height, value) {
  const data = new Uint8ClampedArray(width * height * 4)
  for (let i = 0; i < data.length; i += 4) {
    data[i] = value
    data[i + 1] = value
    data[i + 2] = value
    data[i + 3] = 255
  }
  return { data, width, height }
}
