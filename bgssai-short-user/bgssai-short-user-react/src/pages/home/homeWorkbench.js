import { visibleFailReason } from '../../lib/displayLabels.js'

/** Seeded end-to-end sample owned by the demo account. */
export const SAMPLE_WORK_ID = 'w-dm-005'
export const SAMPLE_WORK_TITLE = '悬疑法医手记'
export const SAMPLE_FLOW_PATH = `/works/${SAMPLE_WORK_ID}`
export const SAMPLE_FLOW_LABEL = '看示例流程'

export function ownedSamplePath(works) {
  const owned = (Array.isArray(works) ? works : []).find(
    (work) => work?.work_id === SAMPLE_WORK_ID || work?.work_title === SAMPLE_WORK_TITLE,
  )
  return owned?.work_id ? `/works/${owned.work_id}` : null
}
export const RECENT_WORK_LIMIT = 6
export const ATTENTION_LIMIT = 6

const COVER_KEYS = ['cover_url', 'cover_image_url', 'poster_url']

const STAGE_FAILURES = [
  { field: 'script_status', tab: 'script', label: '剧本生成失败' },
  { field: 'storyboard_status', tab: 'storyboard', label: '分镜生成失败' },
  { field: 'dubbing_status', tab: 'voice', label: '配音生成失败' },
  { field: 'compose_status', tab: 'compose', label: '成片生成失败' },
]

export function coverUrl(record) {
  if (!record || typeof record !== 'object') return null
  for (const key of COVER_KEYS) {
    const value = record[key]
    if (typeof value !== 'string') continue
    const url = value.trim()
    if (/^(https?:|data:image\/)/i.test(url)) return url
  }
  return null
}

export function episodeProgress(work, episodes) {
  const listed = Array.isArray(episodes) ? episodes : null
  const declared = Number(work?.episode_count)
  const total = Number.isFinite(declared) && declared > 0 ? declared : (listed?.length || 0)
  if (!listed || listed.length === 0) {
    return { total, percent: null, label: total ? `${total} 集` : '' }
  }
  const count = total || listed.length
  const filmed = listed.filter((ep) => ep.compose_status === 'READY' || ep.review_status === 'APPROVED').length
  const scripted = listed.filter((ep) => ep.script_status && ep.script_status !== 'NONE').length
  const done = filmed > 0 ? filmed : scripted
  const percent = Math.max(0, Math.min(100, Math.round((done / count) * 100)))
  const label = filmed > 0 ? `成片 ${filmed}/${count}` : `剧本 ${scripted}/${count}`
  return { total: count, percent, label }
}

function withEpisode(path, episodeId) {
  if (!episodeId) return path
  return `${path}?episode_id=${encodeURIComponent(episodeId)}`
}

function latestFailedStage(episode) {
  let found = null
  for (const stage of STAGE_FAILURES) {
    if (episode?.[stage.field] === 'FAILED') found = stage
  }
  return found
}

export function continuePath(work, episodes) {
  const workId = work?.work_id
  if (!workId) return '/works'
  const list = Array.isArray(episodes) ? episodes : []
  if (!list.length) return `/works/${workId}/script`
  const episode = list.find((ep) => ep.review_status !== 'APPROVED') || list[0]
  const episodeId = episode.episode_id
  if (episode.review_status === 'PENDING' || episode.review_status === 'REJECTED') {
    return withEpisode(`/works/${workId}/compose`, episodeId)
  }
  const failed = latestFailedStage(episode)
  if (failed) return withEpisode(`/works/${workId}/${failed.tab}`, episodeId)
  const script = episode.script_status
  if (!script || script === 'NONE' || script === 'DRAFT' || script === 'GENERATING') {
    return withEpisode(`/works/${workId}/script`, episodeId)
  }
  if (episode.storyboard_status !== 'READY') {
    return withEpisode(`/works/${workId}/storyboard`, episodeId)
  }
  if (episode.dubbing_status !== 'READY') {
    return withEpisode(`/works/${workId}/voice`, episodeId)
  }
  return withEpisode(`/works/${workId}/compose`, episodeId)
}

function taskDetail(task) {
  const reason = visibleFailReason(task)
  if (reason) return reason
  return task?.task_type_name || task?.task_type || ''
}

export function attentionItems({ works, episodesById, failedByWorkId }) {
  const items = []
  for (const work of Array.isArray(works) ? works : []) {
    const workId = work?.work_id
    if (!workId) continue
    const title = work.work_title || '未命名作品'
    const episodes = episodesById?.[workId] || []
    for (const episode of episodes) {
      const no = episode?.episode_no
      const prefix = no != null && no !== '' ? `第${no}集` : '剧集'
      if (episode?.review_status === 'PENDING') {
        items.push({
          key: `pending-${episode.episode_id || no}`,
          kind: 'review_pending',
          workTitle: title,
          title: `${title} · ${prefix}待审核`,
          detail: '',
          action: '去查看',
          to: withEpisode(`/works/${workId}/compose`, episode.episode_id),
        })
      } else if (episode?.review_status === 'REJECTED') {
        items.push({
          key: `rejected-${episode.episode_id || no}`,
          kind: 'review_rejected',
          workTitle: title,
          title: `${title} · ${prefix}审核驳回`,
          detail: '',
          action: '去修改',
          to: withEpisode(`/works/${workId}/compose`, episode.episode_id),
        })
      }
    }
    const failedTasks = failedByWorkId?.[workId] || []
    if (failedTasks.length) {
      const detail = taskDetail(failedTasks[0])
      items.push({
        key: `failed-${workId}`,
        kind: 'generate_failed',
        workTitle: title,
        title: `${title} · 生成失败`,
        detail: failedTasks.length > 1 && !detail ? `${failedTasks.length} 个失败任务` : detail,
        action: '去处理',
        to: `/works/${workId}/tasks`,
      })
    } else {
      for (const episode of episodes) {
        const stage = latestFailedStage(episode)
        if (!stage) continue
        const no = episode.episode_no
        const where = no != null && no !== '' ? `第${no}集` : ''
        items.push({
          key: `stage-${episode.episode_id || no}-${stage.tab}`,
          kind: 'generate_failed',
          workTitle: title,
          title: `${title} · ${where}${stage.label}`,
          detail: '',
          action: '去处理',
          to: withEpisode(`/works/${workId}/${stage.tab}`, episode.episode_id),
        })
        break
      }
    }
  }
  return items.slice(0, ATTENTION_LIMIT)
}

export function nextStepText({ loadError, works, attention }) {
  if (loadError) return '作品列表暂时读不到，刷新后再看下一步。'
  const list = Array.isArray(works) ? works : []
  if (!list.length) return '先新建一部作品。剧名和一句话梗概就够了。'
  const items = Array.isArray(attention) ? attention : []
  const failed = items.find((item) => item.kind === 'generate_failed')
  if (failed) return `先处理《${failed.workTitle}》的生成失败，再继续。`
  const rejected = items.find((item) => item.kind === 'review_rejected')
  if (rejected) return `《${rejected.workTitle}》有审核驳回，先去改。`
  const pending = items.find((item) => item.kind === 'review_pending')
  if (pending) return `《${pending.workTitle}》有剧集在等审核，先去看结果。`
  return `接着做《${list[0].work_title || '最近的作品'}》。`
}
