/** Task statuses that still change on their own and justify a refresh. */
export const ACTIVE_TASK_STATUSES = Object.freeze(['QUEUED', 'RUNNING'])

/**
 * @param {object[]} rows current page of tasks
 * @returns {boolean} true while any visible task is queued or running
 */
export function hasActiveTasks(rows) {
  if (!Array.isArray(rows)) return false
  return rows.some((row) => ACTIVE_TASK_STATUSES.includes(row?.task_status))
}

export const TASK_POLL_INTERVAL_MS = 3000
