/** Task ids whose outcome toast has already been shown in this page session. */
export const autoTaskOutcomeMemory = new Set()

/**
 * Record a task id the first time its outcome should be shown.
 * The same id returns false afterwards, including when a reload gives `load`
 * a new identity and an effect waits an already succeeded task again.
 * A blank id is ignored so a missing task cannot block a later real one.
 */
export function shouldAnnounceTaskOutcome(announcedTaskIds, taskId) {
  if (taskId == null || taskId === '') return false
  if (!announcedTaskIds || typeof announcedTaskIds.has !== 'function' || typeof announcedTaskIds.add !== 'function') {
    return false
  }
  const id = String(taskId)
  if (announcedTaskIds.has(id)) return false
  announcedTaskIds.add(id)
  return true
}

export function hasAnnouncedTaskOutcome(announcedTaskIds, taskId) {
  if (taskId == null || taskId === '') return false
  if (!announcedTaskIds || typeof announcedTaskIds.has !== 'function') return false
  return announcedTaskIds.has(String(taskId))
}

/** Drop the one-shot outline task id from history state. Keep any other keys. */
export function withoutOutlineTaskId(locationState) {
  if (!locationState || typeof locationState !== 'object' || Array.isArray(locationState)) return null
  if (!Object.prototype.hasOwnProperty.call(locationState, 'outlineTaskId')) return locationState
  const next = { ...locationState }
  delete next.outlineTaskId
  return Object.keys(next).length ? next : null
}
