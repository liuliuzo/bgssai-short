import test from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  arrangeAngle,
  buildLibraryLookOptions,
  cardGenerationCopy,
  groupLooks,
  groupLooksOfKind,
  legacyKindRedirectTarget,
  libraryKindCountText,
  libraryOtherKindsNotice,
  libraryRouteForKind,
  librarySummaryCounts,
  librarySummaryText,
  lookCover,
  matchSelectedLookValue,
  referenceLine,
} from './lookGroups.js'
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
  assert.equal(cardGenerationCopy({
    reference: { used: 1, source: 'CARD', library_name: '我的定妆', material_name: '沈知微', view_name: '正脸' },
  }), '生成时会用「我的定妆」里的「沈知微 · 正脸」。')
  assert.equal(cardGenerationCopy({
    reference: { used: 0, source: 'NONE' },
    refImageUrl: "data:image/svg+xml;base64,PHN2Zw==",
  }), '这次没有参考图，只按外观描述生成。')
  assert.match(cardGenerationCopy({
    reference: { used: 0, source: 'NONE' },
    candidates: [{ asset_id: 'a', file_url: 'https://cdn.example/hall.jpg' }],
  }), /还没选定/)
  assert.match(cardGenerationCopy({
    reference: { used: 0, source: 'NONE' },
    refImageUrl: 'https://cdn.example/hall.jpg',
    selectedAssetId: 'a',
    candidates: [{ asset_id: 'a', file_url: 'https://cdn.example/hall.jpg' }],
  }), /已经选定了一张照片/)
  assert.doesNotMatch(cardGenerationCopy({
    reference: { used: 0, source: 'NONE' },
    refImageUrl: 'https://cdn.example/hall.jpg',
  }), /^这次没有参考图/)
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

test('侧面站姿不占主位，改到其他照片', () => {
  const view = arrangeAngle([
    { material_id: 'pose', caption: '白衬衫-侧面-站姿', mime_type: 'image/jpeg', file_url: 'pose' },
    { material_id: 'side', caption: '白衬衫-侧面', mime_type: 'image/jpeg', file_url: 'side' },
  ])
  assert.equal(view.primary.material_id, 'side')
  assert.equal(view.extras[0].material_id, 'pose')
  const onlyPose = arrangeAngle([
    { material_id: 'pose', caption: '白衬衫-侧面-站姿', mime_type: 'image/jpeg', file_url: 'pose' },
  ])
  assert.equal(onlyPose.primary.material_id, 'pose')
  assert.equal(onlyPose.extras.length, 0)
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

test('场景参考按全景主图、中景机位、局部陈设分组，封面优先全景主图', () => {
  const materials = [
    { material_name: '总裁办公室', material_kind: 'SCENE', view_angle: 'DETAIL_VIEW', file_url: 'https://cdn.example/desk.jpg' },
    { material_name: '总裁办公室', material_kind: 'SCENE', view_angle: 'ESTABLISHING', file_url: 'https://cdn.example/wide.jpg' },
    { material_name: '雨夜便利店', material_kind: 'SCENE', view_angle: null, file_url: 'https://cdn.example/legacy.jpg' },
    { material_name: '退婚书', material_kind: 'PROP', view_angle: 'MAIN_PROP', file_url: 'https://cdn.example/prop.jpg' },
    { material_name: '沈知微', material_kind: 'CHARACTER', view_angle: 'FRONT_FACE', file_url: 'https://cdn.example/face.jpg' },
  ]
  const scenes = groupLooksOfKind(materials, 'SCENE')
  assert.equal(scenes.length, 2)
  assert.equal(scenes[0].slots.ESTABLISHING.length, 1)
  assert.equal(scenes[0].slots.DETAIL_VIEW.length, 1)
  assert.equal(scenes[0].slots.MAIN_VIEW.length, 0)
  assert.equal(lookCover(scenes[0]).file_url, 'https://cdn.example/wide.jpg')
  assert.equal(scenes[1].extra.length, 1)
  assert.equal(lookCover(scenes[1]).file_url, 'https://cdn.example/legacy.jpg')

  const props = groupLooksOfKind(materials, 'PROP')
  assert.equal(props.length, 1)
  assert.equal(lookCover(props[0]).file_url, 'https://cdn.example/prop.jpg')

  assert.deepEqual(librarySummaryCounts(materials), {
    characterCount: 1,
    sceneCount: 2,
    propCount: 1,
  })
  assert.equal(librarySummaryText(materials), '1 个造型 · 2 个场景 · 1 个道具')
  assert.equal(libraryKindCountText(materials, 'CHARACTER'), '1 个造型')
  assert.equal(libraryKindCountText(materials, 'SCENE'), '2 个场景')
  assert.equal(libraryKindCountText(materials, 'PROP'), '1 个道具')
  assert.equal(libraryOtherKindsNotice(materials, 'SCENE'), '这个分组里还有 1 个造型、1 个道具，自动匹配开关和归档恢复会一起生效。')
  assert.equal(libraryOtherKindsNotice([{ material_name: '天台', material_kind: 'SCENE' }], 'SCENE'), '')
  assert.equal(libraryRouteForKind('CHARACTER'), '/look-library')
  assert.equal(libraryRouteForKind('SCENE'), '/scene-library')
  assert.equal(libraryRouteForKind('PROP'), '/prop-library')
  assert.equal(legacyKindRedirectTarget('SCENE', '/works/w-1/characters'), '/scene-library?return=%2Fworks%2Fw-1%2Fcharacters')
  assert.equal(legacyKindRedirectTarget('PROP', 'https://evil.example'), '/prop-library')
  assert.equal(legacyKindRedirectTarget('CHARACTER', '/works/w-1/characters'), null)
  assert.equal(legacyKindRedirectTarget(null, '/works/w-1/characters'), null)
})

test('定妆库与角色卡支持正脸及全部四面独立重做、原照片保留与一键换回', () => {
  const dir = path.dirname(fileURLToPath(import.meta.url))
  const lookPage = fs.readFileSync(path.join(dir, 'LookLibraryPage.jsx'), 'utf8')
  const studioPage = fs.readFileSync(path.join(dir, '..', 'works', 'WorkStudio.jsx'), 'utf8')
  const productionApi = fs.readFileSync(path.join(dir, '..', '..', 'api', 'production.js'), 'utf8')

  assert.doesNotMatch(lookPage, /按正脸重做/)
  assert.doesNotMatch(lookPage, /angle !== 'FRONT_FACE'/)
  assert.match(lookPage, /promoteLookMaterial/)
  assert.match(lookPage, /换回上一张/)
  assert.match(lookPage, /换回这张/)
  assert.match(lookPage, /直接重做/)
  assert.match(lookPage, /skip_reference/)

  assert.doesNotMatch(studioPage, /按正脸重做/)
  assert.match(studioPage, /promoteLookMaterial/)
  assert.match(studioPage, /换回这张/)
  assert.match(studioPage, /skip_reference/)

  assert.match(productionApi, /\/user\/look-materials\/promote/)
})

test('页面文案不再使用资产卡或参考卡概念', () => {
  const dir = path.dirname(fileURLToPath(import.meta.url))
  const studioPage = fs.readFileSync(path.join(dir, '..', 'works', 'WorkStudio.jsx'), 'utf8')
  const flowFile = fs.readFileSync(path.join(dir, '..', 'works', 'productionFlow.js'), 'utf8')
  const listUxFile = fs.readFileSync(path.join(dir, '..', 'works', 'studioListUx.js'), 'utf8')
  const cinemaFile = fs.readFileSync(path.join(dir, '..', '..', 'utils', 'CinematographyHelper.js'), 'utf8')

  for (const content of [studioPage, flowFile, listUxFile, cinemaFile]) {
    assert.doesNotMatch(content, /资产卡/)
    assert.doesNotMatch(content, /参考卡/)
  }
})

test('直接选择人物定妆与场景道具参考：聚合选项并移除跳转管理链接', () => {
  const libraries = [
    {
      library_id: 'lk-arch',
      library_name: '归档库',
      status: 'ARCHIVED',
      material_list: [
        {
          material_id: 'lm-arch-1',
          material_name: '旧造型',
          material_kind: 'CHARACTER',
          view_angle: 'FRONT_FACE',
          file_url: 'https://cdn.example/old.jpg',
        },
      ],
    },
    {
      library_id: 'lk-1',
      library_name: '我的定妆',
      status: 'ACTIVE',
      material_list: [
        {
          material_id: 'lm-side',
          material_name: '小明',
          material_kind: 'CHARACTER',
          view_angle: 'SIDE',
          file_url: 'https://cdn.example/side.jpg',
        },
        {
          material_id: 'lm-face',
          material_name: '小明',
          material_kind: 'CHARACTER',
          view_angle: 'FRONT_FACE',
          file_url: 'https://cdn.example/face.jpg',
        },
        {
          material_id: 'lm-scene',
          material_name: '办公室',
          material_kind: 'SCENE',
          view_angle: 'ESTABLISHING',
          file_url: 'https://cdn.example/office.jpg',
        },
      ],
    },
  ]

  const charOpts = buildLibraryLookOptions(libraries, 'CHARACTER')
  assert.equal(charOpts.length, 2)
  assert.equal(charOpts[0].value, 'lm-face')
  assert.equal(charOpts[0].label, '小明（我的定妆）')
  assert.equal(charOpts[0].angleSummary, '正脸 / 侧面')
  assert.deepEqual(charOpts[0].materialIds, ['lm-face', 'lm-side'])
  assert.equal(charOpts[1].archived, true)

  assert.equal(matchSelectedLookValue(charOpts, {
    used: 1,
    source: 'CARD',
    material_id: 'lm-side',
    material_name: '小明',
    library_name: '我的定妆',
  }), 'lm-face')
  assert.equal(matchSelectedLookValue(charOpts, {
    used: 1,
    source: 'CARD',
    material_name: '小明',
    library_name: '我的定妆',
  }), 'lm-face')
  assert.equal(matchSelectedLookValue(charOpts, {
    used: 1,
    source: 'NAME',
    material_name: '小明',
    library_name: '我的定妆',
  }), undefined)

  const dir = path.dirname(fileURLToPath(import.meta.url))
  const pickerSource = fs.readFileSync(path.join(dir, 'LibraryLookPicker.jsx'), 'utf8')
  const studioSource = fs.readFileSync(path.join(dir, '..', 'works', 'WorkStudio.jsx'), 'utf8')

  assert.doesNotMatch(pickerSource, /管理定妆库|管理场景库|管理道具库|管理\{storeLabel\}|libraryHref/)
  assert.doesNotMatch(pickerSource, /<Select|大图点选|收起大图/)
  assert.equal((pickerSource.match(/onUse\(/g) || []).length, 1)
  assert.match(pickerSource, /onUse\(option\.materialId\)/)
  assert.match(pickerSource, /使用中/)
  assert.match(pickerSource, /selectedOption\?\.libraryId/)
  assert.match(pickerSource, /搜索/)

  assert.doesNotMatch(studioSource, /管理定妆库|管理场景库|管理道具库|libraryHref/)
  assert.match(studioSource, /listLookLibraries/)
  assert.match(studioSource, /libraries=\{lookLibraries\}/)
  assert.match(studioSource, /定妆候选：/)
})

test('WorkStudio 用到的 mediaUrl 助手必须全部导入，避免角色页运行时白屏', () => {
  const dir = path.dirname(fileURLToPath(import.meta.url))
  const mediaUrlSource = fs.readFileSync(path.join(dir, '..', '..', 'lib', 'mediaUrl.js'), 'utf8')
  const studioSource = fs.readFileSync(path.join(dir, '..', 'works', 'WorkStudio.jsx'), 'utf8')
  const exported = [...mediaUrlSource.matchAll(/export function (\w+)/g)].map((m) => m[1])
  const importMatch = studioSource.match(/import\s*\{([^}]+)\}\s*from\s*['"]\.\.\/\.\.\/lib\/mediaUrl['"]/)
  const imported = importMatch ? importMatch[1].split(',').map((s) => s.trim()).filter(Boolean) : []
  for (const name of exported) {
    const used = new RegExp(`\\b${name}\\s*\\(`).test(studioSource)
    if (used) assert.ok(imported.includes(name), `WorkStudio.jsx uses ${name} but does not import it`)
  }
})

test('场景卡独立成 scenes 页签，步骤总数不再写死 8', () => {
  const dir = path.dirname(fileURLToPath(import.meta.url))
  const studioSource = fs.readFileSync(path.join(dir, '..', 'works', 'WorkStudio.jsx'), 'utf8')
  const guideSource = fs.readFileSync(path.join(dir, '..', 'works', 'ProductionGuide.jsx'), 'utf8')
  assert.match(studioSource, /effectiveTab === 'scenes'/)
  assert.match(studioSource, /section="scenes"/)
  assert.match(studioSource, /section="characters"/)
  assert.doesNotMatch(studioSource, /8\/8|\/8 \}/)
  assert.doesNotMatch(guideSource, /8\/8|\/8 步/)
  assert.match(studioSource, /VIDEO_PATH_STEPS\.length/)
  assert.match(guideSource, /VIDEO_PATH_STEPS\.length/)
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
