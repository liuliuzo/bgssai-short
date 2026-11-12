export const LOOK_ANGLES = [
  ['FRONT_FACE', '正脸'],
  ['FRONT', '前面'],
  ['BACK', '背面'],
  ['SIDE', '侧面'],
]

const ANGLE_KEYS = new Set(LOOK_ANGLES.map(([value]) => value))

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
