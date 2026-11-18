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

const FAIL_ACTION_BY_TAB = {
  script: '去写剧本',
  storyboard: '去生成视频',
  voice: '去配音',
  compose: '去合成成片',
}

const STAGE_ACTION_BY_KEY = {
  pending_script: '去写剧本',
  script: '去写剧本',
  looks: '去定妆',
  shots: '去拆分镜',
  frame: '去出首帧',
  video: '去生成视频',
  compose: '去合成成片',
}

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

/**
 * Resolve a single episode along the unified 6-stage video production chain:
 * 剧本(1) -> 定妆(2) -> 分镜(3) -> 首帧(4) -> 视频(5) -> 成片(6).
 */
export function resolveEpisodeStage(episode, detail) {
  const outlineReady = Number(detail?.outline_count) > 0
  const lookTotal = Number(detail?.look_total) || 0
  const lookLocked = Math.min(lookTotal, Math.max(0, Number(detail?.look_locked) || 0))
  const hasShotStats = episode?.shot_total != null
  const shotTotal = Math.max(0, Number(episode?.shot_total) || 0)
  const shotFrameReady = Math.min(shotTotal, Math.max(0, Number(episode?.shot_frame_ready) || 0))
  const shotReady = Math.min(shotTotal, Math.max(0, Number(episode?.shot_ready) || 0))
  const shotSpokenReady = episode?.shot_spoken_ready != null
    ? Math.max(0, Number(episode.shot_spoken_ready) || 0)
    : shotTotal
  const filmExpired = Number(episode?.film_expired_flag) === 1
  const scriptStatus = episode?.script_status || 'NONE'
  const scriptReady = scriptStatus === 'READY' || scriptStatus === 'LOCKED'

  if (!scriptReady) {
    if (!scriptStatus || scriptStatus === 'NONE') {
      return {
        key: 'pending_script',
        stageLabel: '待写剧本',
        tab: 'script',
        units: outlineReady ? 0.25 : 0,
        done: false,
      }
    }
    return {
      key: 'script',
      stageLabel: '剧本',
      tab: 'script',
      units: scriptStatus === 'DRAFT' ? 0.7 : 0.5,
      done: false,
    }
  }

  if (lookTotal > 0 && lookLocked < lookTotal) {
    return {
      key: 'looks',
      stageLabel: `定妆 ${lookLocked}/${lookTotal}`,
      tab: 'characters',
      units: 1 + (lookLocked / lookTotal),
      done: false,
    }
  }

  if (hasShotStats) {
    if (shotTotal === 0) {
      return { key: 'shots', stageLabel: '分镜', tab: 'storyboard', units: 2.2, done: false }
    }
    if (shotFrameReady < shotTotal && shotSpokenReady === 0) {
      return { key: 'shots', stageLabel: '分镜', tab: 'storyboard', units: 2.6, done: false }
    }
    if (shotFrameReady < shotTotal) {
      return {
        key: 'frame',
        stageLabel: `首帧 ${shotFrameReady}/${shotTotal}`,
        tab: 'storyboard',
        units: 3 + (shotFrameReady / shotTotal),
        done: false,
      }
    }
    if (shotReady < shotTotal) {
      return {
        key: 'video',
        stageLabel: `视频 ${shotReady}/${shotTotal}`,
        tab: 'storyboard',
        units: 4 + (shotReady / shotTotal),
        done: false,
      }
    }
    if ((episode?.compose_status === 'READY' || episode?.review_status === 'APPROVED') && !filmExpired) {
      return { key: 'done', stageLabel: '已成片', tab: 'compose', units: 6, done: true }
    }
    if (episode?.compose_status === 'COMPOSING') {
      return { key: 'compose', stageLabel: '合成中', tab: 'compose', units: 5.5, done: false }
    }
    return { key: 'compose', stageLabel: '待合成', tab: 'compose', units: 5, done: false }
  }

  if (
    (episode?.compose_status === 'READY' || episode?.review_status === 'APPROVED')
    && !filmExpired
    && episode?.storyboard_status !== 'FAILED'
  ) {
    return { key: 'done', stageLabel: '已成片', tab: 'compose', units: 6, done: true }
  }
  if (episode?.compose_status === 'COMPOSING') {
    return { key: 'compose', stageLabel: '合成中', tab: 'compose', units: 5.5, done: false }
  }
  if (episode?.storyboard_status === 'READY') {
    return { key: 'compose', stageLabel: '待合成', tab: 'compose', units: 5, done: false }
  }
  if (episode?.storyboard_status && episode.storyboard_status !== 'NONE') {
    return { key: 'video', stageLabel: '视频', tab: 'storyboard', units: 4, done: false }
  }
  return { key: 'shots', stageLabel: '分镜', tab: 'storyboard', units: 2.2, done: false }
}

/**
 * One list row per episode, in the order the API returned them. No range
 * folding or truncation here: the full list backs the all-episodes view, so
 * every episode stays individually reachable however many episodes a work has.
 * The card itself shows episodeCardSummary(): the whole list when short, or
 * the next episode plus stage counts when long.
 */
export function episodeStageItems(work, episodes, detail) {
  const list = Array.isArray(episodes) ? episodes : []
  if (!list.length) return []
  const workId = work?.work_id
  return list.map((episode, idx) => {
    const no = episode?.episode_no != null && episode.episode_no !== '' ? episode.episode_no : (idx + 1)
    const stage = resolveEpisodeStage(episode, detail)
    const tone = stage.done ? 'done' : (stage.key === 'pending_script' ? 'todo' : 'active')
    const to = workId ? withEpisode(`/works/${workId}/${stage.tab}`, episode?.episode_id) : '/works'
    return {
      key: `ep-${episode?.episode_id || no}`,
      no,
      noLabel: `第${no}集`,
      stageLabel: stage.stageLabel,
      text: `第${no}集：${stage.stageLabel}`,
      tone,
      to,
    }
  })
}

/** Works with at most this many episodes list every episode on the card itself. */
export const CARD_EPISODE_INLINE_LIMIT = 4

/**
 * Card-space view of the episode list. Short lists stay fully inline; longer
 * ones fold to the next unfinished episode plus stage counts, with the full
 * list one click away, so card height never depends on the episode count.
 * `continueTo` is the card's primary-action target: when it names an
 * unfinished episode, that episode is the one kept visible.
 */
export function episodeCardSummary(episodeItems, continueTo) {
  const items = Array.isArray(episodeItems) ? episodeItems : []
  const counts = { done: 0, active: 0, todo: 0 }
  for (const item of items) {
    if (item?.tone === 'done') counts.done += 1
    else if (item?.tone === 'todo') counts.todo += 1
    else counts.active += 1
  }
  const countsLabel = `已成片 ${counts.done} · 制作中 ${counts.active} · 待写剧本 ${counts.todo}`
  const total = items.length
  if (total <= CARD_EPISODE_INLINE_LIMIT) {
    return { total, folded: false, nextItem: null, inlineItems: items, counts, countsLabel }
  }
  const firstPending = items.find((item) => item?.tone !== 'done') || null
  const target = continueTo ? items.find((item) => item?.to === continueTo) : null
  const nextItem = firstPending && target && target.tone !== 'done' ? target : firstPending
  return {
    total,
    folded: true,
    nextItem,
    inlineItems: nextItem ? [nextItem] : [],
    counts,
    countsLabel,
  }
}

/** Locate one episode row by a user-typed episode number (`57` or `第57集`). */
export function findEpisodeItem(episodeItems, query) {
  const items = Array.isArray(episodeItems) ? episodeItems : []
  const text = query == null ? '' : String(query).trim()
  if (!text || text.startsWith('-')) return null
  const match = text.match(/\d+/)
  if (!match) return null
  const no = Number(match[0])
  if (!Number.isInteger(no) || no <= 0) return null
  return items.find((item) => Number(item?.no) === no) || null
}

export function episodeProgress(work, episodes, detail) {
  const listed = Array.isArray(episodes) ? episodes : null
  const declared = Number(work?.episode_count)
  const total = Number.isFinite(declared) && declared > 0 ? declared : (listed?.length || 0)
  if (!listed) {
    return { total, filmed: 0, percent: null, label: total ? `共 ${total} 集` : '', episodeItems: [] }
  }
  if (listed.length === 0) {
    return {
      total,
      filmed: 0,
      percent: total ? 0 : null,
      label: total ? `总进度 0% · 已成片 0/${total} 集` : '',
      episodeItems: [],
    }
  }
  const count = Math.max(total || 0, listed.length)
  const stages = listed.map((ep) => resolveEpisodeStage(ep, detail))
  const filmed = stages.filter((stage) => stage.done).length
  const padCount = Math.max(0, count - listed.length)
  const padUnits = padCount * (Number(detail?.outline_count) > 0 ? 0.25 : 0)
  const totalUnits = stages.reduce((sum, stage) => sum + stage.units, 0) + padUnits
  const raw = Math.round((totalUnits / (count * 6)) * 100)
  const percent = filmed >= count ? 100 : (totalUnits > 0 ? Math.max(1, Math.min(99, raw)) : 0)
  const label = `总进度 ${percent}% · 已成片 ${filmed}/${count} 集`
  return {
    total: count,
    filmed,
    percent,
    label,
    episodeItems: episodeStageItems(work, listed, detail),
  }
}

export function workCardStatus(work, episodes, detail) {
  if (work?.work_status === 'ARCHIVED') return '已归档'
  if (work?.work_status === 'COMPLETED') return '已完成'
  if (!Array.isArray(episodes) || episodes.length === 0) {
    return work?.work_status_name || ''
  }
  const count = Math.max(Number(work?.episode_count) || 0, episodes.length)
  const stages = episodes.map((ep) => resolveEpisodeStage(ep, detail))
  if (stages.length >= count && stages.every((stage) => stage.done)) {
    if (episodes.every((ep) => ep?.review_status === 'APPROVED')) return '已完成'
    if (episodes.some((ep) => ep?.review_status === 'PENDING')) return '审核中'
    return '已成片'
  }
  if (episodes.some((ep) => ep?.compose_status === 'COMPOSING')) return '合成中'
  if (episodes.some((ep) => ep?.review_status === 'PENDING')) return '审核中'
  if (stages.every((stage) => stage.key === 'pending_script') && !(Number(detail?.outline_count) > 0)) {
    return '草稿'
  }
  if (stages.every((stage) => stage.key === 'pending_script' || stage.key === 'script')) {
    return stages.every((stage) => stage.key === 'pending_script') && work?.work_status === 'DRAFT'
      ? '草稿'
      : '剧本中'
  }
  return '制作中'
}

function resolveContinueTarget(work, episodes, detail) {
  const workId = work?.work_id
  if (!workId) {
    return { path: '/works', actionLabel: '继续创作', episode: null, allDone: false }
  }
  const list = Array.isArray(episodes) ? episodes : []
  if (!list.length) {
    return { path: `/works/${workId}/script`, actionLabel: '去写剧本', episode: null, allDone: false }
  }
  const entries = list.map((ep) => ({
    ep,
    stage: resolveEpisodeStage(ep, detail),
    failed: latestFailedStage(ep),
  }))
  const declared = Number(work?.episode_count)
  const expectedCount = Number.isFinite(declared) && declared > 0 ? Math.max(declared, list.length) : list.length
  const allDone = entries.every((entry) => entry.stage.done) && list.length >= expectedCount

  const rejected = entries.find((entry) => entry.ep?.review_status === 'REJECTED')
  if (rejected) {
    return {
      path: withEpisode(`/works/${workId}/compose`, rejected.ep?.episode_id),
      actionLabel: '去修改成片',
      episode: rejected.ep,
      allDone,
    }
  }

  const failedEntry = entries.find((entry) => entry.failed && !entry.stage.done)
  if (failedEntry) {
    return {
      path: withEpisode(`/works/${workId}/${failedEntry.failed.tab}`, failedEntry.ep?.episode_id),
      actionLabel: FAIL_ACTION_BY_TAB[failedEntry.failed.tab] || '继续创作',
      episode: failedEntry.ep,
      allDone,
    }
  }

  const pendingEntry = entries.find((entry) => !entry.stage.done)
  if (pendingEntry) {
    return {
      path: withEpisode(`/works/${workId}/${pendingEntry.stage.tab}`, pendingEntry.ep?.episode_id),
      actionLabel: STAGE_ACTION_BY_KEY[pendingEntry.stage.key] || '继续创作',
      episode: pendingEntry.ep,
      allDone,
    }
  }

  const pendingReview = entries.find((entry) => entry.ep?.review_status === 'PENDING')
  if (pendingReview) {
    return {
      path: withEpisode(`/works/${workId}/compose`, pendingReview.ep?.episode_id),
      actionLabel: '去看成片',
      episode: pendingReview.ep,
      allDone,
    }
  }

  const first = entries[0]
  return {
    path: withEpisode(`/works/${workId}/compose`, first?.ep?.episode_id),
    actionLabel: '去看成片',
    episode: first?.ep || null,
    allDone,
  }
}

export function continuePath(work, episodes, detail) {
  return resolveContinueTarget(work, episodes, detail).path
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
          statusText: `${prefix}待审核`,
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
          statusText: `${prefix}审核驳回`,
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
        statusText: '生成失败',
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
          statusText: `${where}${stage.label}`,
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
  if (loadError) return '作品列表加载失败'
  const list = Array.isArray(works) ? works : []
  if (!list.length) return '暂无作品'
  const items = Array.isArray(attention) ? attention : []
  const failed = items.find((item) => item.kind === 'generate_failed')
  if (failed) return `《${failed.workTitle}》生成失败`
  const rejected = items.find((item) => item.kind === 'review_rejected')
  if (rejected) return `《${rejected.workTitle}》审核驳回`
  const pending = items.find((item) => item.kind === 'review_pending')
  if (pending) return `《${pending.workTitle}》待审核`
  return `最近作品：《${list[0].work_title || '未命名作品'}》`
}

export function continueLabel(work, episodes, detail) {
  const target = resolveContinueTarget(work, episodes, detail)
  const list = Array.isArray(episodes) ? episodes : []
  const declared = Number(work?.episode_count)
  const totalCount = Math.max(Number.isFinite(declared) && declared > 0 ? declared : 0, list.length)
  const epNo = target.episode?.episode_no
  if (totalCount > 1 && !target.allDone && epNo != null && epNo !== '') {
    return `第${epNo}集 · ${target.actionLabel}`
  }
  return target.actionLabel
}
