/** One next step from a new episode to a generated shot video. No new API. */

const OPEN = new Set(['QUEUED', 'RUNNING'])

export const VIDEO_PATH_STEPS = [
  { key: 'outline', label: '大纲' },
  { key: 'script', label: '剧本' },
  { key: 'ready', label: '定稿' },
  { key: 'looks', label: '定妆' },
  { key: 'shots', label: '拆镜' },
  { key: 'frame', label: '首帧' },
  { key: 'video', label: '视频' },
  { key: 'film', label: '成片' },
]

const STEP_TAB = {
  outline: 'script',
  script: 'script',
  ready: 'script',
  looks: 'characters',
  shots: 'storyboard',
  frame: 'storyboard',
  video: 'storyboard',
  film: 'compose',
}

function text(value) {
  if (value == null) return ''
  return String(value).trim()
}

export function shotNeedsSpoken(row) {
  const spoken = [row?.previz_text, row?.image_prompt, row?.shot_desc].map(text).join('')
  return spoken.trim().length < 6
}

export function shotHasFrame(row) {
  return text(row?.first_frame_asset_id) !== '' || text(row?.first_frame_url) !== ''
}

export function shotHasVideo(row) {
  return text(row?.video_asset_id) !== ''
    || text(row?.video_url) !== ''
    || row?.shot_status === 'VIDEO_READY'
    || row?.shot_status === 'LOCKED'
}

export function isCardLocked(row) {
  return row?.card_status === 'LOCKED' && text(row?.ref_image_asset_id) !== ''
}

export function unlockedLooks(panel) {
  const list = []
  for (const row of panel?.character_list || []) {
    if (isCardLocked(row)) continue
    const id = text(row?.character_id)
    if (!id) continue
    list.push({
      kind: 'character',
      id,
      name: text(row.character_name) || '未命名角色',
      candidateId: text((row.candidate_list || [])[0]?.asset_id),
    })
  }
  for (const row of [...(panel?.scene_list || []), ...(panel?.prop_list || [])]) {
    if (isCardLocked(row)) continue
    const id = text(row?.scene_card_id)
    if (!id) continue
    list.push({
      kind: 'scene',
      id,
      name: text(row.card_name) || '未命名',
      candidateId: text((row.candidate_list || [])[0]?.asset_id),
    })
  }
  return list
}

function running(tasks, pred) {
  return (tasks || []).some((row) => OPEN.has(row?.task_status) && pred(row))
}

export function buildVideoPathInput({ outline, script, panel, shots, tasks, episode } = {}) {
  const outlineList = Array.isArray(outline?.outline) ? outline.outline : []
  const sceneList = Array.isArray(script?.scene_list) ? script.scene_list : []
  const shotRows = Array.isArray(shots) ? shots : []
  const episodeId = episode?.episode_id
  const sameEpisode = (row) => !episodeId || row.episode_id === episodeId
  return {
    episodeNo: episode?.episode_no,
    episodeId,
    outlineCount: outlineList.length,
    outlineRunning: running(tasks, (row) => row.target_type === 'OUTLINE'),
    outlineFailed: (tasks || []).some((row) => row.target_type === 'OUTLINE' && row.task_status === 'FAILED'),
    scriptStatus: script?.script_status || episode?.script_status || 'NONE',
    sceneCount: sceneList.length,
    totalScore: script?.total_score,
    scriptRunning: episode?.script_status === 'GENERATING'
      || running(tasks, (row) => row.target_type === 'SCRIPT' && sameEpisode(row)),
    looks: unlockedLooks(panel),
    shots: shotRows,
    shotCount: shotRows.length,
    videoCount: shotRows.filter(shotHasVideo).length,
    imageShotRunning: running(tasks, (row) => row.target_type === 'SHOT' && row.task_type === 'IMAGE' && sameEpisode(row)),
    videoShotRunning: running(tasks, (row) => row.target_type === 'SHOT' && row.task_type === 'VIDEO' && sameEpisode(row)),
    portraitRunning: running(tasks, (row) => row.target_type === 'PORTRAIT' || row.target_type === 'SCENE_PORTRAIT'),
    composeStatus: episode?.compose_status || 'NONE',
  }
}

export function shotsForFrames(shots) {
  return (shots || []).filter((row) => row?.shot_id
    && row.shot_status !== 'LOCKED'
    && !shotHasVideo(row)
    && !shotHasFrame(row)
    && !shotNeedsSpoken(row))
}

export function shotsForVideo(shots) {
  return (shots || []).filter((row) => row?.shot_id
    && row.shot_status !== 'LOCKED'
    && shotHasFrame(row)
    && !shotHasVideo(row))
}

function doneBefore(key) {
  const index = VIDEO_PATH_STEPS.findIndex((item) => item.key === key)
  return VIDEO_PATH_STEPS.slice(0, Math.max(0, index)).map((item) => item.key)
}

function episodeTitle(no, title) {
  if (no == null || no === '') return title
  return `第${no}集：${title}`
}

function step(input, key, extra) {
  return {
    key,
    tab: STEP_TAB[key],
    doneKeys: extra.doneKeys || doneBefore(key),
    waiting: !!extra.waiting,
    action: extra.action || '',
    secondary: extra.secondary || null,
    kind: extra.kind || 'open',
    title: episodeTitle(input.episodeNo, extra.title),
    detail: extra.detail || '',
    look: extra.look || null,
  }
}

export function resolveVideoPath(input) {
  const data = input || {}
  if (!data.outlineCount) {
    if (data.outlineRunning) {
      return step(data, 'outline', {
        kind: 'wait',
        waiting: true,
        title: '大纲正在生成',
        detail: '好了之后就可以写这一集的剧本。这一页会自己刷新。',
      })
    }
    return step(data, 'outline', {
      kind: 'outline',
      title: data.outlineFailed ? '大纲没有生成成功' : '先生成分集大纲',
      detail: data.outlineFailed
        ? '上次没有生成成功。再提交一次，成功后才写剧本。'
        : '大纲是后面所有步骤的起点。生成完再写这一集的剧本。',
      action: data.outlineFailed ? '重新生成大纲' : '生成大纲',
    })
  }

  const scriptReady = data.scriptStatus === 'READY' || data.scriptStatus === 'LOCKED'
  if (!scriptReady) {
    if (data.scriptRunning) {
      return step(data, 'script', {
        kind: 'wait',
        waiting: true,
        title: '剧本正在生成',
        detail: '好了之后定稿。定稿才会抽出角色和场景。',
      })
    }
    if (!data.sceneCount || data.scriptStatus === 'NONE' || data.scriptStatus === 'FAILED' || data.scriptStatus === 'GENERATING') {
      return step(data, 'script', {
        kind: 'script',
        title: data.scriptStatus === 'FAILED' ? '剧本没有生成成功' : '接着写这一集的剧本',
        detail: '没有剧本就没有镜头，也就出不了视频。',
        action: data.scriptStatus === 'FAILED' ? '重新生成剧本' : '生成这一集剧本',
      })
    }
    const score = Number(data.totalScore)
    const low = Number.isFinite(score) && score > 0 && score < 60
    return step(data, 'ready', {
      kind: 'ready',
      title: '把这一集剧本定稿',
      detail: low
        ? `节奏分 ${score}，不到 60，直接定稿会被拦住。可以用强制定稿，这一步不花钱。`
        : '定稿之后才会抽出角色、场景和道具。这一步不花钱。',
      action: '定稿这一集',
      secondary: { kind: 'ready-force', action: '强制定稿' },
    })
  }

  const looks = Array.isArray(data.looks) ? data.looks : []
  if (looks.length) {
    if (data.portraitRunning) {
      return step(data, 'looks', {
        kind: 'wait',
        waiting: true,
        title: '定妆图正在生成',
        detail: '生成完再锁定。没锁定的卡，出图和出视频都会失败。',
      })
    }
    const first = looks[0]
    const names = looks.slice(0, 3).map((item) => item.name).join('、')
    if (first.candidateId) {
      return step(data, 'looks', {
        kind: 'lock',
        look: first,
        title: `锁定「${first.name}」的定妆`,
        detail: `还没定妆的有 ${names}。已有图的先锁定，不用再花钱。`,
        action: `锁定「${first.name}」`,
      })
    }
    return step(data, 'looks', {
      kind: 'portrait',
      look: first,
      title: `先给「${first.name}」定妆`,
      detail: `还没定妆的有 ${names}。会出 2 张并锁定第一张。没锁定的卡，后面的出图和出视频都会失败。`,
      action: `给「${first.name}」出定妆`,
    })
  }

  const shots = Array.isArray(data.shots) ? data.shots : []
  if (!data.shotCount) {
    return step(data, 'shots', {
      kind: 'split',
      title: '把剧本拆成镜头',
      detail: '拆镜不花钱，也不会直接出视频。拆完再出首帧。',
      action: '从剧本拆镜',
    })
  }

  const unframed = shots.filter((row) => row?.shot_id
    && row.shot_status !== 'LOCKED'
    && !shotHasVideo(row)
    && !shotHasFrame(row))
  if (unframed.length && unframed.every(shotNeedsSpoken)) {
    return step(data, 'shots', {
      kind: 'previz',
      title: '先写好人话分镜',
      detail: '有镜头还没有画面描述，出图会被拦住。这一步不花钱。',
      action: '写好人话分镜',
    })
  }
  if (unframed.length) {
    if (data.imageShotRunning) {
      return step(data, 'frame', {
        kind: 'wait',
        waiting: true,
        title: '首帧正在生成',
        detail: '先别重复提交。好了之后才能生成视频。',
      })
    }
    return step(data, 'frame', {
      kind: 'frame',
      title: '先出首帧，才能生成视频',
      detail: '没有首帧的镜头，生成视频会被直接拒绝。确认费用后才出图。',
      action: '出首帧',
    })
  }
  const videoCount = data.videoCount != null ? data.videoCount : shots.filter(shotHasVideo).length
  if (videoCount < data.shotCount) {
    if (data.videoShotRunning) {
      return step(data, 'video', {
        kind: 'wait',
        waiting: true,
        title: '视频正在生成',
        detail: '生成通常要几分钟。先别重复提交。',
      })
    }
    return step(data, 'video', {
      kind: 'video',
      title: '可以生成镜头视频了',
      detail: '只提交已经有首帧、还没有视频的镜头。确认费用后才开始。',
      action: '生成视频',
    })
  }
  if (data.composeStatus === 'READY') {
    return step(data, 'film', {
      kind: 'open',
      title: '这一集已经有成片',
      detail: '可以在成片页下载，或发布到 Tube。',
      action: '查看成片',
      doneKeys: VIDEO_PATH_STEPS.map((item) => item.key),
    })
  }
  return step(data, 'film', {
    kind: 'open',
    title: '镜头视频已经齐了',
    detail: '下一步是把这一集合成一条成片。配音可以后补，不挡住合成入口。',
    action: '去合成成片',
  })
}
