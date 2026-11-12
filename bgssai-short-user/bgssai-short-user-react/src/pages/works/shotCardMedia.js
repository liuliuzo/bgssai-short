/** List-card media decisions. A bound clip stays playable even when the latest video job failed. */

import { SHOT_STATUS_LABEL, labelOf } from '../../lib/displayLabels.js'
import { isPlaceholderMediaUrl } from '../../lib/mediaUrl.js'
import { shotTimingLabel } from './studioListUx.js'

const SETTLED_CLIP_STATUS = new Set(['VIDEO_READY', 'LOCKED'])

export function shotHasBoundVideo(row) {
  if (!row || typeof row !== 'object') return false
  if (isPlaceholderMediaUrl(row.video_url)) return false
  return text(row.video_url) !== '' || text(row.video_asset_id) !== ''
}

export function shotHasPlayableVideo(row) {
  const url = text(row && row.video_url)
  return url !== '' && !isPlaceholderMediaUrl(url)
}

/**
 * A decoded frame with no positive duration is not a successful clip.
 * pending: metadata has not arrived. ready: a real length. broken: empty or errored.
 */
export function classifyVideoDuration(duration, { errored = false, hasFrame = false } = {}) {
  if (errored) return 'broken'
  if (Number.isFinite(duration) && duration > 0.05) return 'ready'
  if (duration === 0) return 'broken'
  if (hasFrame) return 'broken'
  return 'pending'
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
export function shotCardHeading(row, { actualSec, playback } = {}) {
  const no = row && row.shot_no != null ? row.shot_no : ''
  const planned = shotTimingLabel({ plannedSec: row && row.shot_duration_sec, hasPlayableVideo: false })
  if (isPlaceholderMediaUrl(row && row.video_url) || playback === 'placeholder') {
    return `镜 ${no} · 计划 ${planned} · 这是占位图，不能播放`
  }
  if (playback === 'broken') {
    return `镜 ${no} · 计划 ${planned} · 这段视频打不开`
  }
  const timing = shotTimingLabel({
    plannedSec: row && row.shot_duration_sec,
    actualSec,
    hasPlayableVideo: shotHasPlayableVideo(row),
  })
  const status = labelOf(SHOT_STATUS_LABEL, shotCardStatusCode(row))
  return `镜 ${no} · ${timing} · ${status}`
}

export function shotLatestTaskBadge(row) {
  if (isPlaceholderMediaUrl(row && row.video_url)) {
    return { color: 'orange', text: '这是占位图，不能播放' }
  }
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
