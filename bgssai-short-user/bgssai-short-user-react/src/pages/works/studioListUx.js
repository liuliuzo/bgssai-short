import { formatTokenAmount } from '../../utils/formatToken.js'
import {
  CAMERA_MOVEMENTS,
  CONTINUITY_PRESETS,
  DRAMA_STYLES,
  QUAD_GRID_ANGLES,
} from '../../utils/CinematographyHelper.js'

/** Above the in-page support button (support-widget.css z-index 1200). */
export const SHOT_VIDEO_DIALOG_Z_INDEX = 2000

/** Drawer and image preview must sit above the support button. */
export const SHOT_SURFACE_Z_INDEX = 1400

const AMBIENT_DIALOGUE = '（纯动作环境音，无对白）'
const RAW_CARD_ID = /^(?:ch|sc|pr|prop)[-_][a-z0-9-]+$/i

/** Native controls sit in this bottom band; picture clicks above it toggle playback. */
export const LIST_PLAYER_CONTROL_BAND = 48

/** Stable list slot. Bound clips on the live page are 720×480; the poster must not resize the row. */
export const LIST_PLAYER_ASPECT = '3 / 2'

const EMPTY_SCENE = '未写清场景'

const PLACE_SUFFIXES = [
  '宴会厅', '办公室', '会议室', '客厅', '卧室', '走廊', '别墅', '门口',
  '餐厅', '酒店', '医院', '学校', '书房', '厨房', '阳台', '花园',
  '公寓', '房间', '大厅', '天台', '小区', '豪宅', '街道', '街上',
  '车里', '车上', '家里', '公司',
]

const PLACE_STOP = '，。、；：！？,.;到在从去进来回往向把被让的了'

export function listPlayerFrameStyle() {
  return {
    position: 'relative',
    width: '100%',
    aspectRatio: LIST_PLAYER_ASPECT,
    background: '#000',
    borderRadius: 8,
    overflow: 'hidden',
  }
}

export function listPlayerVideoStyle() {
  return {
    position: 'absolute',
    inset: 0,
    width: '100%',
    height: '100%',
    objectFit: 'contain',
    background: '#000',
    display: 'block',
  }
}

export function listPlayerToggleStyle() {
  return {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: LIST_PLAYER_CONTROL_BAND,
    border: 0,
    padding: 0,
    margin: 0,
    background: 'transparent',
    cursor: 'pointer',
  }
}

export function pictureClickTogglesPlayback(offsetY, height, band = LIST_PLAYER_CONTROL_BAND) {
  if (!Number.isFinite(offsetY) || !Number.isFinite(height) || height <= 0) return false
  if (height <= band) return true
  return offsetY < height - band
}

export function playbackAction(paused) {
  return paused ? 'play' : 'pause'
}

export function shotDrawerProps() {
  return { zIndex: SHOT_SURFACE_Z_INDEX }
}

/** A shot card must scroll away as a whole; descendants cannot pin to the page scroller. */
export function shotCardContainment() {
  return {
    position: 'relative',
    overflow: 'clip',
    overflowAnchor: 'none',
  }
}

export function thumbPlaceholderStyle() {
  return {
    background: '#e8edf3',
    borderRadius: 8,
  }
}

/** Narrow first-frame thumb under a playable list clip. Do not widen it to fit the caption. */
export const LIST_FIRST_FRAME_THUMB_WIDTH = 72

/** One line at 11px inside LIST_FIRST_FRAME_THUMB_WIDTH. Click still opens the same preview. */
export const FIRST_FRAME_CAPTION = '查看首帧'

export const LAST_FRAME_CAPTION = '查看尾帧'

/** CJK and fullwidth punctuation take one em; ASCII takes half. */
export function captionEmWidth(caption) {
  let em = 0
  for (const ch of String(caption || '')) {
    em += ch.charCodeAt(0) <= 0xff ? 0.5 : 1
  }
  return em
}

/** True when the caption stays on one line inside the thumb, with no orphan glyph. */
export function frameCaptionFits(caption, thumbWidth = LIST_FIRST_FRAME_THUMB_WIDTH, fontSize = 11) {
  const text = String(caption || '')
  if (!text || /[\r\n]/.test(text)) return false
  return captionEmWidth(text) * fontSize <= thumbWidth
}

export function frameCaptionStyle() {
  return {
    display: 'block',
    width: '100%',
    marginTop: 2,
    padding: 0,
    border: 0,
    background: 'transparent',
    color: '#64748b',
    fontSize: 11,
    lineHeight: 1.4,
    cursor: 'pointer',
    textAlign: 'left',
    whiteSpace: 'nowrap',
  }
}

export function outlineLongTextProps() {
  return { autoSize: { minRows: 2, maxRows: 8 } }
}

export function bindSelectProps() {
  return {
    virtual: false,
    optionFilterProp: 'label',
    popupMatchSelectWidth: false,
  }
}

export function bindOptionLabel(name, statusName) {
  const raw = String(name || '').trim()
  const clean = RAW_CARD_ID.test(raw) ? '' : raw
  const status = String(statusName || '').trim()
  const base = clean || '未命名'
  return status ? `${base} ${status}` : base
}

export function optionTextLeaksRawId(text) {
  return /(?:^|\s)(?:ch|sc|pr|prop)[-_][a-z0-9-]+/i.test(String(text || ''))
}

export function shotSpokenText(row, sceneName, draft) {
  const known = knownSceneName({
    sceneName,
    shotDesc: row?.shot_desc,
    lookPlain: row?.look_plain,
  })
  return reconcileEmptyScene(draft ?? row?.previz_text ?? '', known)
}

export function shotDialogueLine(row) {
  const direct = String(row?.dialogue_text || row?.dialogue || '').trim()
  if (direct) return direct
  const lines = Array.isArray(row?.line_list)
    ? row.line_list.map((line) => String(line?.line_text || '').trim()).filter(Boolean)
    : []
  if (lines.length) return lines.join(' / ')
  const previz = String(row?.previz_text || '')
  const mark = '台词：'
  const at = previz.indexOf(mark)
  if (at >= 0) {
    const sentence = previz.slice(at + mark.length).trim().split(/[。\n]/)[0].trim()
    if (sentence && !/^无(对白)?$/.test(sentence)) return sentence
  }
  return ''
}

export function dialogueCaption(row) {
  return shotDialogueLine(row) || AMBIENT_DIALOGUE
}

export function dubbingAlignLabel(row) {
  const status = String(row?.align_status || '')
  const lines = Array.isArray(row?.line_list) ? row.line_list.length : 0
  if (status === 'ALIGNED') return '已对齐'
  if (status === 'MISALIGNED') return '未对齐'
  if (lines === 0) return '未配音'
  if (status === 'GENERATING') return '生成中'
  return '待对齐'
}

export function voiceRowView(row, actualSec) {
  const dialogue = shotDialogueLine(row)
  const hasVideo = Boolean(row?.video_url) || positiveSeconds(actualSec) != null
  return {
    dialogue,
    dialogueCaption: dialogue || AMBIENT_DIALOGUE,
    timing: shotTimingLabel({
      plannedSec: row?.shot_duration_sec,
      actualSec,
      hasPlayableVideo: hasVideo,
    }),
    align: dubbingAlignLabel(row),
  }
}

export function showSettledEmpty(ready, count) {
  return ready === true && !(Number(count) > 0)
}

export function shotVideoDialogProps() {
  return {
    zIndex: SHOT_VIDEO_DIALOG_Z_INDEX,
    centered: true,
    width: 720,
    destroyOnClose: true,
    footer: null,
    maskClosable: true,
    keyboard: true,
    getContainer: typeof document === 'undefined' ? undefined : () => document.body,
    styles: {
      mask: { backgroundColor: 'rgba(15, 23, 42, 0.78)' },
      content: { padding: 0, overflow: 'hidden' },
      header: { marginBottom: 0, padding: '14px 16px' },
      body: { background: '#000', padding: 0 },
    },
  }
}

export function enlargePlaybackButtonProps() {
  return {
    type: 'default',
    size: 'small',
    block: true,
    htmlType: 'button',
  }
}

function positiveSeconds(value) {
  const n = Number(value)
  if (!Number.isFinite(n) || n <= 0) return null
  return n
}

function formatSecondLabel(seconds) {
  return `${Math.round(Number(seconds))}秒`
}

function secondsNear(left, right) {
  return Math.round(left) === Math.round(right)
}

/**
 * Planned length stays visible. A playable clip whose real duration differs
 * must not be labeled with only the plan.
 */
export function shotTimingLabel({ plannedSec, actualSec, hasPlayableVideo }) {
  const plannedShown = positiveSeconds(plannedSec) == null ? 3 : positiveSeconds(plannedSec)
  const actual = positiveSeconds(actualSec)
  if (!hasPlayableVideo) return formatSecondLabel(plannedShown)
  if (actual == null) return `计划 ${formatSecondLabel(plannedShown)}`
  if (secondsNear(plannedShown, actual)) return formatSecondLabel(actual)
  return `成片 ${formatSecondLabel(actual)} · 计划 ${formatSecondLabel(plannedShown)}`
}

export function tokenAmountLabel(tokens) {
  return `${formatTokenAmount(tokens)} 额度`
}

export function selectionFeeLine(selectedCount, billedTokens) {
  const count = Math.max(0, Math.trunc(Number(selectedCount) || 0))
  return `勾选镜头后估费用（当前勾选 ${count} 镜 · 预计 ${tokenAmountLabel(billedTokens)}）。生成前会再弹窗确认预计费用与余额。`
}

export function modelSelectProps(label) {
  const title = String(label || '').trim()
  return {
    title,
    popupMatchSelectWidth: false,
    style: { width: 360 },
    dropdownStyle: { minWidth: 360 },
  }
}

export function shotPresetGroups() {
  return [
    {
      key: 'camera',
      label: '运镜',
      items: CAMERA_MOVEMENTS.slice(1, 6).map((item) => ({
        key: item.value,
        label: item.label.split(' ')[0],
        marker: `【运镜：${item.label}】`,
      })),
    },
    {
      key: 'style',
      label: '风格',
      items: DRAMA_STYLES.slice(0, 3).map((item) => ({
        key: item.key,
        label: item.label.split(' ')[0],
        marker: `【基调：${item.label}】`,
      })),
    },
    {
      key: 'angle',
      label: '视角',
      items: QUAD_GRID_ANGLES.map((item) => {
        const label = item.label.split(' ')[0]
        return { key: item.value, label, marker: `【机位：${label}】` }
      }),
    },
    {
      key: 'continuity',
      label: '衔接写法',
      items: CONTINUITY_PRESETS.map((item) => ({
        key: item.value,
        label: item.label,
        marker: `【衔接：${item.label}】`,
      })),
    },
  ]
}

export function frameLinkOptions() {
  return [
    { value: 'NONE', label: '不衔接' },
    { value: 'INHERIT_PREV', label: '接上一镜' },
    { value: 'BOOKEND', label: '首尾帧' },
  ]
}

export function appendPreset(text, marker) {
  const raw = String(text || '')
  const token = String(marker || '')
  if (!token || raw.includes(token)) return raw
  return raw ? `${raw}，${token}` : token
}

export function presetIsActive(text, marker) {
  const token = String(marker || '')
  return token !== '' && String(text || '').includes(token)
}

function isCjk(ch) {
  return /[\u4e00-\u9fa5]/.test(ch)
}

export function placeFromNarrative(text) {
  const source = String(text || '')
  if (!source) return ''
  let best = ''
  for (const suffix of PLACE_SUFFIXES) {
    let from = 0
    while (from < source.length) {
      const idx = source.indexOf(suffix, from)
      if (idx < 0) break
      let start = idx
      while (start > 0) {
        const ch = source[start - 1]
        if (!isCjk(ch) || PLACE_STOP.includes(ch)) break
        if (idx + suffix.length - (start - 1) > 8) break
        start -= 1
      }
      const phrase = source.slice(start, idx + suffix.length)
      if (phrase.length > best.length) best = phrase
      from = idx + suffix.length
    }
  }
  return best
}

export function knownSceneName({ sceneName, shotDesc, lookPlain } = {}) {
  const fromDesc = placeFromNarrative(shotDesc)
  if (fromDesc) return fromDesc
  const fromLook = placeFromNarrative(lookPlain)
  if (fromLook) return fromLook
  const card = String(sceneName || '').trim()
  if (!card || card === EMPTY_SCENE || /^sc[-_]/i.test(card)) return ''
  return card
}

export function reconcileEmptyScene(previzText, scene) {
  const raw = String(previzText || '')
  const known = String(scene || '').trim()
  if (!known || known === EMPTY_SCENE || !raw.includes(`在哪：${EMPTY_SCENE}`)) return raw
  return raw.split(`在哪：${EMPTY_SCENE}`).join(`在哪：${known}`)
}

export function assetBinderPlaceholder(kind, count, ready = true) {
  if (!ready) return `正在读取${kind}`
  const n = Math.max(0, Math.trunc(Number(count) || 0))
  if (!n) return `还没有${kind}`
  return `绑定${kind}（${n}）`
}

export function assetLibrarySummary({ characters, scenes, props, ready = true } = {}) {
  if (!ready) return '正在读取可绑定的角色、场景和道具。'
  const parts = []
  const characterNames = namesOf(characters)
  const sceneNames = namesOf(scenes)
  const propNames = namesOf(props)
  if (characterNames.length) parts.push(`角色 ${characterNames.join('、')}`)
  if (sceneNames.length) parts.push(`场景 ${sceneNames.join('、')}`)
  if (propNames.length) parts.push(`道具 ${propNames.join('、')}`)
  if (!parts.length) return '还没有可绑定的角色、场景或道具。'
  return `本剧可绑定：${parts.join(' · ')}。下拉框用来勾选镜头后写入，空白表示还没选要绑定的卡。`
}

function namesOf(list) {
  if (!Array.isArray(list)) return []
  return list.map((item) => String(item || '').trim()).filter(Boolean)
}
