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

export function lookCover(look) {
  if (!look) return null
  for (const [angle] of LOOK_ANGLES) {
    const item = look.slots[angle] && look.slots[angle][0]
    if (item && (item.poster_url || item.file_url)) return item
  }
  return (look.extra || []).find((item) => item.poster_url || item.file_url) || null
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
