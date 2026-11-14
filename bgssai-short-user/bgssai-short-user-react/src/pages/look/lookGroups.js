import { isPlaceholderMediaUrl, isRealPhotoUrl } from '../../lib/mediaUrl.js'
import { safeReturnPath } from '../../lib/safeReturn.js'

export const LOOK_ANGLES = [
  ['FRONT_FACE', '正脸'],
  ['FRONT', '前面'],
  ['BACK', '背面'],
  ['SIDE', '侧面'],
]

export const SCENE_ANGLES = [
  ['ESTABLISHING', '全景主图'],
  ['MAIN_VIEW', '中景机位'],
  ['DETAIL_VIEW', '局部陈设'],
]

export const PROP_ANGLES = [
  ['MAIN_PROP', '主视图'],
  ['DETAIL_PROP', '特写细节'],
]

export const LOOK_KINDS = [
  { key: 'CHARACTER', label: '人物定妆', pageTitle: '定妆库', route: '/look-library', itemNoun: '造型', cardNoun: '角色' },
  { key: 'SCENE', label: '场景参考', pageTitle: '场景库', route: '/scene-library', itemNoun: '场景', cardNoun: '场景' },
  { key: 'PROP', label: '道具参考', pageTitle: '道具库', route: '/prop-library', itemNoun: '道具', cardNoun: '道具' },
]

export function normalizeLookKind(kind) {
  const upper = String(kind || '').trim().toUpperCase()
  if (upper === 'SCENE' || upper === 'PROP') return upper
  return 'CHARACTER'
}

export function libraryRouteForKind(kind) {
  const normalized = normalizeLookKind(kind)
  if (normalized === 'SCENE') return '/scene-library'
  if (normalized === 'PROP') return '/prop-library'
  return '/look-library'
}

export function legacyKindRedirectTarget(kindParam, returnParam) {
  const upper = String(kindParam || '').trim().toUpperCase()
  if (upper !== 'SCENE' && upper !== 'PROP') return null
  const base = libraryRouteForKind(upper)
  const back = safeReturnPath(returnParam)
  return back ? `${base}?return=${encodeURIComponent(back)}` : base
}

export function anglesForKind(kind) {
  const normalized = normalizeLookKind(kind)
  if (normalized === 'SCENE') return SCENE_ANGLES
  if (normalized === 'PROP') return PROP_ANGLES
  return LOOK_ANGLES
}

export function primaryAngleForKind(kind) {
  const normalized = normalizeLookKind(kind)
  if (normalized === 'SCENE') return 'ESTABLISHING'
  if (normalized === 'PROP') return 'MAIN_PROP'
  return 'FRONT_FACE'
}

export function emptySlotsForKind(kind) {
  const slots = {}
  for (const [angle] of anglesForKind(kind)) {
    slots[angle] = []
  }
  return slots
}

export function groupLooksOfKind(materials, kind) {
  const wanted = normalizeLookKind(kind)
  return groupLooks(
    (materials || []).filter((item) => normalizeLookKind(item && item.material_kind) === wanted),
    wanted,
  )
}

export function librarySummaryCounts(materials) {
  return {
    characterCount: groupLooksOfKind(materials, 'CHARACTER').length,
    sceneCount: groupLooksOfKind(materials, 'SCENE').length,
    propCount: groupLooksOfKind(materials, 'PROP').length,
  }
}

export function librarySummaryText(materials) {
  const { characterCount, sceneCount, propCount } = librarySummaryCounts(materials)
  const parts = [`${characterCount} 个造型`, `${sceneCount} 个场景`]
  if (propCount > 0) parts.push(`${propCount} 个道具`)
  return parts.join(' · ')
}

export function libraryKindCountText(materials, kind) {
  const normalized = normalizeLookKind(kind)
  const { characterCount, sceneCount, propCount } = librarySummaryCounts(materials)
  if (normalized === 'SCENE') return `${sceneCount} 个场景`
  if (normalized === 'PROP') return `${propCount} 个道具`
  return `${characterCount} 个造型`
}

export function libraryOtherKindsNotice(materials, kind) {
  const normalized = normalizeLookKind(kind)
  const { characterCount, sceneCount, propCount } = librarySummaryCounts(materials)
  const others = []
  if (normalized !== 'CHARACTER' && characterCount > 0) others.push(`${characterCount} 个造型`)
  if (normalized !== 'SCENE' && sceneCount > 0) others.push(`${sceneCount} 个场景`)
  if (normalized !== 'PROP' && propCount > 0) others.push(`${propCount} 个道具`)
  if (!others.length) return ''
  return `这个分组里还有 ${others.join('、')}，自动匹配开关和归档恢复会一起生效。`
}

export function isVideoMaterial(item) {
  return !!(item && typeof item.mime_type === 'string' && item.mime_type.startsWith('video/'))
}

function hasPicture(item) {
  return !!(item && (item.file_url || item.poster_url))
}

function photoKey(item) {
  const size = item && item.file_size_byte
  const width = item && item.width
  const height = item && item.height
  if (!size || !width || !height) return null
  return `${item.mime_type || ''}|${width}|${height}|${size}`
}

/** Identical bytes filed from more than one card stay as one photo. */
export function dedupePhotos(items) {
  const kept = []
  const index = new Map()
  for (const item of items || []) {
    const key = photoKey(item)
    if (!key) {
      kept.push(item)
      continue
    }
    const previous = index.get(key)
    if (previous) {
      previous.same_material_ids.push(item.material_id)
      continue
    }
    const copy = { ...item, same_material_ids: [item.material_id] }
    index.set(key, copy)
    kept.push(copy)
  }
  return kept
}

/** 站姿、坐姿是补充，不能盖住这一面本来的那张。 */
export function isPoseExtra(item) {
  return /站姿|坐姿|行走|走动/.test(`${(item && item.caption) || ''}`)
}

/**
 * One angle: a plain still is the photo people see first.
 * A pose note (站姿) stays in 其他照片 when a plain photo of this angle exists.
 * A reference video is never that photo — its cover is one frame and can be a blurry crop.
 */
export function arrangeAngle(items) {
  const stills = []
  const videos = []
  const missing = []
  for (const item of dedupePhotos(items)) {
    if (isVideoMaterial(item)) videos.push(item)
    else if (hasPicture(item)) stills.push(item)
    else missing.push(item)
  }
  const plain = []
  const pose = []
  for (const item of stills) {
    if (isPoseExtra(item)) pose.push(item)
    else plain.push(item)
  }
  const ordered = plain.length ? [...plain, ...pose] : stills
  return {
    primary: ordered[0] || null,
    extras: ordered.slice(1),
    videos,
    missing,
  }
}

export function lookCover(look) {
  if (!look) return null
  const slots = look.slots || {}
  const priority = ['FRONT_FACE', 'ESTABLISHING', 'MAIN_PROP']
  const orderedKeys = [
    ...priority.filter((key) => Object.prototype.hasOwnProperty.call(slots, key)),
    ...Object.keys(slots).filter((key) => !priority.includes(key)),
  ]
  const pools = orderedKeys.map((angle) => slots[angle] || [])
  pools.push(look.extra || [])
  for (const pool of pools) {
    const still = pool.find((item) => !isVideoMaterial(item) && hasPicture(item))
    if (still) return still
  }
  for (const pool of pools) {
    const any = pool.find((item) => hasPicture(item))
    if (any) return any
  }
  return null
}

export function referenceImage(ref) {
  if (!ref || (ref.used !== 1 && ref.used !== true)) return null
  const video = typeof ref.mime_type === 'string' && ref.mime_type.startsWith('video/')
  const url = video ? ref.poster_url : (ref.file_url || ref.poster_url)
  if (!url) return null
  return { url, label: ref.source === 'NAME' ? '生成时参考' : '参考图' }
}

export function referenceLine(ref, textOnly) {
  if (textOnly) return '这次不使用参考图，只按外观描述生成。'
  if (!ref || (ref.used !== 1 && ref.used !== true) || ref.source === 'NONE') {
    return '这次没有参考图，只按外观描述生成。'
  }
  const library = ref.library_name || '我的定妆'
  const look = ref.material_name || '未命名'
  const angle = ref.view_name ? ` · ${ref.view_name}` : ''
  if (ref.source === 'NAME') {
    return `「${library}」里的「${look}${angle}」和外观描述比较接近，生成时会用这张。可以换一张，或改成只按文字生成。`
  }
  return `生成时会用「${library}」里的「${look}${angle}」。`
}

function realCandidatePhotos(candidates) {
  return (Array.isArray(candidates) ? candidates : []).filter((item) => isRealPhotoUrl(item && item.file_url))
}

/**
 * Portrait generation follows the library hit. A selected card photo is not that hit.
 * Seed SVG tiles are not photos, so they must not make the "no reference" line look false.
 */
export function cardGenerationCopy({ reference, refImageUrl, candidates, selectedAssetId, textOnly } = {}) {
  if (textOnly) return referenceLine(reference, true)
  const ref = reference || {}
  if ((ref.used === 1 || ref.used === true) && ref.source && ref.source !== 'NONE') {
    return referenceLine(ref)
  }
  const photos = realCandidatePhotos(candidates)
  const selectedReal = photos.some((item) => item.asset_id && item.asset_id === selectedAssetId)
    || isRealPhotoUrl(refImageUrl)
  if (selectedReal) {
    return '这张卡上已经选定了一张照片，但这次生成不会用它，只按外观描述。'
  }
  if (photos.length) {
    return '这次没有参考图，只按外观描述生成。下面的照片还没选定，这次生成也不会用它们。'
  }
  if (isPlaceholderMediaUrl(refImageUrl)) {
    return '这次没有参考图，只按外观描述生成。'
  }
  return referenceLine(ref)
}

export function groupLooks(materials, kind = 'CHARACTER') {
  const angles = anglesForKind(kind)
  const angleKeys = new Set(angles.map(([value]) => value))
  const order = []
  const groups = new Map()
  for (const item of materials || []) {
    const name = (item && item.material_name) || '未命名'
    if (!groups.has(name)) {
      groups.set(name, {
        name,
        kind: normalizeLookKind(kind),
        slots: emptySlotsForKind(kind),
        extra: [],
      })
      order.push(name)
    }
    const group = groups.get(name)
    const angle = item && item.view_angle
    if (angleKeys.has(angle)) group.slots[angle].push(item)
    else group.extra.push(item)
  }
  return order.map((name) => groups.get(name))
}
