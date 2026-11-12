import { isPlaceholderMediaUrl, isRealPhotoUrl } from '../../lib/mediaUrl.js'

export const LOOK_ANGLES = [
  ['FRONT_FACE', '正脸'],
  ['FRONT', '前面'],
  ['BACK', '背面'],
  ['SIDE', '侧面'],
]

const ANGLE_KEYS = new Set(LOOK_ANGLES.map(([value]) => value))

export function groupLooksOfKind(materials, kind) {
  const wanted = kind || 'CHARACTER'
  return groupLooks((materials || []).filter((item) => (item.material_kind || 'CHARACTER') === wanted))
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

/**
 * One angle: the newest still is the photo people see first.
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
  return {
    primary: stills[0] || null,
    extras: stills.slice(1),
    videos,
    missing,
  }
}

export function lookCover(look) {
  if (!look) return null
  const pools = LOOK_ANGLES.map(([angle]) => (look.slots[angle] || []))
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
  const library = ref.library_name || '定妆知识库'
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

export function groupLooks(materials) {
  const order = []
  const groups = new Map()
  for (const item of materials || []) {
    const name = (item && item.material_name) || '未命名'
    if (!groups.has(name)) {
      groups.set(name, {
        name,
        slots: { FRONT_FACE: [], FRONT: [], BACK: [], SIDE: [] },
        extra: [],
      })
      order.push(name)
    }
    const group = groups.get(name)
    const angle = item && item.view_angle
    if (ANGLE_KEYS.has(angle)) group.slots[angle].push(item)
    else group.extra.push(item)
  }
  return order.map((name) => groups.get(name))
}
