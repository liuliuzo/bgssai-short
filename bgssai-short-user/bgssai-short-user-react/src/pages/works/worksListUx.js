export function workStudioPath(workId) {
  return `/works/${workId}/script`
}

export function formatWorkTime(value) {
  if (value == null || value === '') return ''
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}/.test(value)) {
    return value.replace('T', ' ').slice(0, 16)
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (n) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

/** Stable distinguisher when several works share a title. */
export function workListMeta(row) {
  const id = row?.work_id ? String(row.work_id) : ''
  const time = formatWorkTime(row?.updated_time)
  return [id, time].filter(Boolean).join(' · ')
}
