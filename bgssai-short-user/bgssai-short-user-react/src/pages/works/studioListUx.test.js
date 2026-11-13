import test from 'node:test'
import assert from 'node:assert/strict'
import {
  LIST_PLAYER_ASPECT,
  LIST_PLAYER_CONTROL_BAND,
  SHOT_VIDEO_DIALOG_Z_INDEX,
  appendPreset,
  assetBinderPlaceholder,
  assetLibrarySummary,
  bindOptionLabel,
  bindSelectProps,
  dialogueCaption,
  dubbingAlignLabel,
  enlargePlaybackButtonProps,
  frameLinkOptions,
  knownSceneName,
  listPlayerFrameStyle,
  optionTextLeaksRawId,
  outlineLongTextProps,
  listPlayerToggleStyle,
  modelSelectProps,
  pictureClickTogglesPlayback,
  playbackAction,
  placeFromNarrative,
  presetIsActive,
  reconcileEmptyScene,
  selectionFeeLine,
  shotCardContainment,
  shotDialogueLine,
  shotDrawerProps,
  shotPresetGroups,
  shotSpokenText,
  shotTimingLabel,
  shotVideoDialogProps,
  showSettledEmpty,
  FIRST_FRAME_CAPTION,
  LAST_FRAME_CAPTION,
  LIST_FIRST_FRAME_THUMB_WIDTH,
  captionEmWidth,
  frameCaptionFits,
  frameCaptionStyle,
  thumbPlaceholderStyle,
  subtitleClock,
  voiceRowView,
  voiceStudioSummary,
  tokenAmountLabel,
} from './studioListUx.js'

const STORED = '谁：重生回到豪门别墅。在哪：未写清场景。做什么：重生回到豪门别墅，她决定先摸清所有人的底牌。怎么拍：中景，固定不动。什么感觉：冷静克制。'

test('clicking the picture toggles playback and the control band does not', () => {
  assert.equal(pictureClickTogglesPlayback(20, 134), true)
  assert.equal(pictureClickTogglesPlayback(134 - LIST_PLAYER_CONTROL_BAND, 134), false)
  assert.equal(pictureClickTogglesPlayback(130, 134), false)
  assert.equal(playbackAction(true), 'play')
  assert.equal(playbackAction(false), 'pause')
})

test('list player keeps one frame so a vertical poster cannot resize the row', () => {
  const frame = listPlayerFrameStyle()
  const toggle = listPlayerToggleStyle()
  assert.equal(frame.aspectRatio, LIST_PLAYER_ASPECT)
  assert.equal(frame.overflow, 'hidden')
  assert.equal(frame.position, 'relative')
  assert.equal(toggle.bottom, LIST_PLAYER_CONTROL_BAND)
  assert.equal(toggle.cursor, 'pointer')
})

test('enlarge control is a button and the dialog stacks above the page', () => {
  const button = enlargePlaybackButtonProps()
  assert.equal(button.type, 'default')
  assert.equal(button.block, true)
  assert.notEqual(button.type, 'link')
  const dialog = shotVideoDialogProps()
  assert.ok(dialog.zIndex > 1200)
  assert.equal(dialog.zIndex, SHOT_VIDEO_DIALOG_Z_INDEX)
  assert.equal(dialog.centered, true)
  assert.ok(dialog.width >= 640)
  assert.match(dialog.styles.mask.backgroundColor, /0\.78/)
  assert.equal(dialog.styles.body.background, '#000')
})

test('clip length replaces a plan label that would contradict the player', () => {
  assert.equal(shotTimingLabel({ plannedSec: 3, hasPlayableVideo: false }), '3秒')
  assert.equal(shotTimingLabel({ plannedSec: 3, hasPlayableVideo: true }), '计划 3秒')
  assert.equal(shotTimingLabel({ plannedSec: 3, actualSec: 6, hasPlayableVideo: true }), '成片 6秒 · 计划 3秒')
  assert.equal(shotTimingLabel({ plannedSec: 6, actualSec: 6.2, hasPlayableVideo: true }), '6秒')
  assert.equal(shotTimingLabel({ plannedSec: null, actualSec: 6, hasPlayableVideo: true }), '成片 6秒 · 计划 3秒')
})

test('zero token estimate keeps the unit', () => {
  assert.equal(tokenAmountLabel(0), '0 额度')
  assert.equal(tokenAmountLabel(1500), '1.5K 额度')
  assert.match(selectionFeeLine(0, 0), /当前勾选 0 镜 · 预计 0 额度/)
})

test('selected model title is the full label', () => {
  const props = modelSelectProps('BGSSAI Image · 按任务实际对账')
  assert.equal(props.title, 'BGSSAI Image · 按任务实际对账')
  assert.equal(props.popupMatchSelectWidth, false)
  assert.ok(props.style.width >= 320)
})

test('preset groups keep continuity choices readable and separate from frame link', () => {
  const groups = shotPresetGroups()
  assert.deepEqual(groups.map((group) => group.label), ['运镜', '风格', '视角', '衔接写法'])
  const continuity = groups.find((group) => group.key === 'continuity')
  assert.ok(continuity.items.some((item) => item.label.includes('同场次首尾帧')))
  assert.ok(continuity.items.some((item) => item.label.includes('跨场景硬切')))
  const link = frameLinkOptions()
  assert.equal(link[0].label, '不衔接')
  assert.equal(link.some((item) => item.label.includes('同场次')), false)
  const marker = continuity.items[0].marker
  assert.equal(presetIsActive('', marker), false)
  const once = appendPreset('谁：沈知微。', marker)
  assert.equal(appendPreset(once, marker), once)
  assert.equal(presetIsActive(once, marker), true)
})

test('known scene replaces 未写清场景 and does not invent one', () => {
  const look = '观众会看到中景：重生回到豪门别墅，她决定先摸清所有人的底牌。镜头固定不动。'
  assert.equal(placeFromNarrative('重生回到豪门别墅，她决定先摸清所有人的底牌'), '豪门别墅')
  assert.equal(placeFromNarrative('会议室里，沈知微把收购案推过桌面'), '会议室')
  assert.equal(knownSceneName({
    sceneName: '宴会厅',
    shotDesc: '重生回到豪门别墅，她决定先摸清所有人的底牌',
    lookPlain: look,
  }), '豪门别墅')
  assert.equal(knownSceneName({ sceneName: '总裁办公室', shotDesc: '她把文件推了过去', lookPlain: '' }), '总裁办公室')
  assert.equal(knownSceneName({ sceneName: 'sc-dm-002-02', shotDesc: '她抬眼', lookPlain: '' }), '')
  const filled = reconcileEmptyScene(STORED, '豪门别墅')
  assert.match(filled, /在哪：豪门别墅/)
  assert.equal(filled.includes('未写清场景'), false)
  assert.equal(reconcileEmptyScene(STORED, ''), STORED)
  assert.equal(reconcileEmptyScene('在哪：走廊。', '豪门别墅'), '在哪：走廊。')
})

test('asset binder shows that cards exist when nothing is selected yet', () => {
  assert.equal(assetBinderPlaceholder('角色', 4), '绑定角色（4）')
  assert.equal(assetBinderPlaceholder('场景', 0), '还没有场景')
  const line = assetLibrarySummary({
    characters: ['沈知微', '顾景深'],
    scenes: ['宴会厅', '总裁办公室'],
    props: [],
  })
  assert.match(line, /沈知微/)
  assert.match(line, /宴会厅/)
  assert.match(line, /下拉框/)
  assert.equal(assetBinderPlaceholder('角色', 0, false), '正在读取角色')
  assert.match(assetLibrarySummary({ ready: false }), /正在读取/)
  assert.equal(assetLibrarySummary({ ready: false }).includes('还没有'), false)
})

test('canvas drawer and voice use the same scene, dialogue, and clip length as the list', () => {
  const shot = {
    dialogue_text: '退婚书我签，但有个条件。',
    previz_text: STORED + '台词：退婚书我签，但有个条件。',
    shot_desc: '重生回到豪门别墅，她决定先摸清所有人的底牌',
    look_plain: '观众会看到中景：重生回到豪门别墅。',
    shot_duration_sec: 3,
    video_url: 'https://example.test/shot.mp4',
    line_list: [],
    align_status: 'PENDING',
    align_status_name: '生成中',
  }
  const spoken = shotSpokenText(shot, '宴会厅')
  assert.match(spoken, /在哪：豪门别墅/)
  assert.equal(spoken.includes('未写清场景'), false)
  assert.equal(shotDialogueLine(shot), '退婚书我签，但有个条件。')
  assert.equal(dialogueCaption({ previz_text: '谁：她。在哪：宴会厅。做什么：推门。' }), '（纯动作环境音，无对白）')
  const office = shotSpokenText({
    previz_text: '谁：女主。在哪：未写清场景。做什么：推门。',
    shot_desc: '女主带着资源回归',
  }, '总裁办公室')
  assert.match(office, /在哪：总裁办公室/)
  const voice = voiceRowView(shot, 6)
  assert.equal(voice.dialogue, '退婚书我签，但有个条件。')
  assert.equal(voice.timing, '成片 6秒 · 计划 3秒')
  assert.equal(voice.align, '未配音')
  assert.equal(dubbingAlignLabel({ align_status: 'GENERATING', line_list: [{ line_text: '退婚书我签' }] }), '生成中')
  assert.equal(dubbingAlignLabel({ align_status: 'ALIGNED', line_list: [{ line_text: '有声' }] }), '已对齐')
})

test('bind options stay human and empty states wait until the request settles', () => {
  const label = bindOptionLabel('沈知微', '已锁定')
  assert.equal(label, '沈知微 已锁定')
  assert.equal(optionTextLeaksRawId(label), false)
  assert.equal(optionTextLeaksRawId(bindOptionLabel('ch-dm-002-01', '已锁定')), false)
  assert.equal(bindOptionLabel('sc-dm-002-02', '已锁定').includes('sc-dm-002-02'), false)
  const select = bindSelectProps()
  assert.equal(select.virtual, false)
  assert.equal(showSettledEmpty(false, 0), false)
  assert.equal(showSettledEmpty(true, 0), true)
  assert.equal(showSettledEmpty(true, 6), false)
})

test('shot cards, thumbs, outline fields, and the drawer stay clear of the page chrome', () => {
  const card = shotCardContainment()
  assert.equal(card.overflow, 'clip')
  assert.equal(card.overflowAnchor, 'none')
  assert.notEqual(thumbPlaceholderStyle().background, '#111')
  assert.equal(thumbPlaceholderStyle().background, '#e8edf3')
  assert.ok(outlineLongTextProps().autoSize.minRows >= 2)
  assert.ok(shotDrawerProps().zIndex > 1200)
})

test('first-frame caption stays one line inside the narrow thumb', () => {
  assert.equal(LIST_FIRST_FRAME_THUMB_WIDTH, 72)
  assert.equal(FIRST_FRAME_CAPTION, '查看首帧')
  assert.equal(LAST_FRAME_CAPTION, '查看尾帧')
  assert.equal(frameCaptionFits(FIRST_FRAME_CAPTION, LIST_FIRST_FRAME_THUMB_WIDTH), true)
  assert.equal(frameCaptionFits(LAST_FRAME_CAPTION, LIST_FIRST_FRAME_THUMB_WIDTH), true)
  const oldCaption = '首帧，点击查看'
  assert.equal(frameCaptionFits(oldCaption, LIST_FIRST_FRAME_THUMB_WIDTH), false)
  assert.equal(captionEmWidth(oldCaption.slice(0, -1)) * 11 <= LIST_FIRST_FRAME_THUMB_WIDTH, true)
  const style = frameCaptionStyle()
  assert.equal(style.whiteSpace, 'nowrap')
  assert.equal(style.fontSize, 11)
  assert.equal(style.width, '100%')
})

test('voice summary does not call pending rows 未对齐 0, and subtitle times are seconds', () => {
  const rows = [
    { align_status: 'PENDING', line_list: [{ line_text: '退婚书我签' }] },
    { align_status: 'PENDING', line_list: [{ line_text: '你凭什么' }] },
  ]
  const summary = voiceStudioSummary(rows, 0)
  assert.match(summary, /待对齐/)
  assert.doesNotMatch(summary, /未对齐 0/)
  assert.equal(subtitleClock(1500), '1.5 秒')
  assert.equal(subtitleClock(null), '—')
})
