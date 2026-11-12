/** List-card media decisions. A bound clip stays playable even when the latest video job failed. */

const SETTLED_CLIP_STATUS = new Set(['VIDEO_READY', 'LOCKED'])

export function shotHasBoundVideo(row) {
  if (!row || typeof row !== 'object') return false
  return text(row.video_url) !== '' || text(row.video_asset_id) !== ''
}

export function shotHasPlayableVideo(row) {
  return text(row && row.video_url) !== ''
}

/** Title status. A bound clip reads as 视频, including while a later attempt is generating or failed. */
export function shotCardStatusCode(row) {
  if (!row || typeof row !== 'object') return 'PENDING'
  if (row.shot_status === 'LOCKED') return 'LOCKED'
  if (shotHasBoundVideo(row)) return 'VIDEO_READY'
  return row.shot_status || 'PENDING'
}

/**
 * Latest-task chip. A failed or canceled VIDEO job must not read as 「视频 已失败」
 * when a clip is already bound.
 * Once the shot has settled on that clip (VIDEO_READY / LOCKED), the historical
 * failure is not the standing label. An in-flight attempt can still show it.
 */
export function shotLatestTaskBadge(row) {
  const task = row && row.latest_task
  if (!task || typeof task !== 'object') return null
  const type = String(task.task_type || '')
  const status = String(task.task_status || '')
  if (shotHasBoundVideo(row) && type === 'VIDEO' && (status === 'FAILED' || status === 'CANCELED')) {
    if (SETTLED_CLIP_STATUS.has(String(row.shot_status || ''))) return null
    return {
      color: 'gold',
      text: status === 'CANCELED' ? '已有视频，本次生成已取消' : '已有视频，本次重生成失败',
    }
  }
  const typeName = task.task_type_name || task.task_type || ''
  const statusName = task.task_status_name || task.task_status || ''
  let color = 'blue'
  if (status === 'FAILED') color = 'red'
  else if (status === 'SUCCEEDED') color = 'green'
  return { color, text: `${typeName} ${statusName}`.trim() }
}

/** Canvas color key. A bound clip keeps the video color instead of falling through to 未开始. */
export function shotCanvasStatusKey(row) {
  if (!row || typeof row !== 'object') return 'PENDING'
  if (row.shot_status === 'LOCKED') return 'LOCKED'
  if (shotHasBoundVideo(row)) return 'VIDEO_READY'
  if (row.blocked_flag === 1) return 'FAILED'
  return row.shot_status || 'PENDING'
}

function text(value) {
  if (value == null) return ''
  return String(value).trim()
}
