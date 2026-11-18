/** One next step from a new episode to a generated shot video. No new API. */

const OPEN = new Set(['QUEUED', 'RUNNING'])

export const SCRIPT_READY_MIN_SCORE = 60

export function resolveScriptRhythmScore(scriptOrScore) {
  const raw = scriptOrScore != null && typeof scriptOrScore === 'object'
    ? (scriptOrScore.rhythm_check?.total_score ?? scriptOrScore.total_score ?? scriptOrScore.totalScore)
    : scriptOrScore
  if (raw == null || (typeof raw === 'string' && raw.trim() === '')) return null
  const score = Number(raw)
  return Number.isFinite(score) ? score : null
}

export function scriptReadyDecision(scriptOrScore) {
  const score = resolveScriptRhythmScore(scriptOrScore)
  if (score !== null && score >= SCRIPT_READY_MIN_SCORE) {
    return {
      needsConfirm: false,
      forceReadyFlag: 0,
      score,
    }
  }
  return {
    needsConfirm: true,
    forceReadyFlag: 1,
    score,
    confirmTitle: '确认定稿',
    confirmContent: score !== null && score > 0
      ? `当前节奏评分 ${score} 分（未满 60 分），确认定稿？`
      : '当前节奏评分未满 60 分，确认定稿？',
    okText: '定稿',
    cancelText: '取消',
  }
}

export const VIDEO_PATH_STEPS = [
  { key: 'outline', stepNo: 1, label: '大纲', tab: 'script' },
  { key: 'script', stepNo: 2, label: '剧本', tab: 'script' },
  { key: 'ready', stepNo: 3, label: '定稿', tab: 'script' },
  { key: 'looks', stepNo: 4, label: '定妆', tab: 'characters' },
  { key: 'scenes', stepNo: 5, label: '场景', tab: 'scenes' },
  { key: 'shots', stepNo: 6, label: '镜头制作', tab: 'storyboard' },
  { key: 'film', stepNo: 7, label: '成片', tab: 'compose' },
]

export const CORE_STUDIO_TABS = new Set(['script', 'characters', 'scenes', 'storyboard', 'compose'])

export const AUX_STUDIO_TOOLS = [
  { key: 'source', label: '原著导入' },
  { key: 'voice', label: '配音字幕' },
  { key: 'preview', label: '样片预览' },
  { key: 'story-map', label: '剧情图谱' },
  { key: 'quality', label: '质量报告' },
  { key: 'continuity', label: '连贯性检查' },
  { key: 'tasks', label: '本剧任务' },
  { key: 'edit-export', label: '导出剪辑工程' },
]

const STEP_BY_KEY = Object.fromEntries(VIDEO_PATH_STEPS.map((item) => [item.key, item]))

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

function unlockedSceneCards(rows) {
  const list = []
  for (const row of rows || []) {
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
  return [...list, ...unlockedSceneCards(panel?.prop_list)]
}

export function unlockedScenes(panel) {
  return unlockedSceneCards(panel?.scene_list)
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
    totalScore: resolveScriptRhythmScore(script),
    scriptRunning: episode?.script_status === 'GENERATING'
      || running(tasks, (row) => row.target_type === 'SCRIPT' && sameEpisode(row)),
    looks: unlockedLooks(panel),
    sceneRefs: unlockedScenes(panel),
    shots: shotRows,
    shotCount: shotRows.length,
    videoCount: shotRows.filter(shotHasVideo).length,
    imageShotRunning: running(tasks, (row) => row.target_type === 'SHOT' && row.task_type === 'IMAGE' && sameEpisode(row)),
    videoShotRunning: running(tasks, (row) => row.target_type === 'SHOT' && row.task_type === 'VIDEO' && sameEpisode(row)),
    portraitRunning: running(tasks, (row) => row.target_type === 'PORTRAIT' || row.target_type === 'SCENE_PORTRAIT'),
    scenePortraitRunning: running(tasks, (row) => row.target_type === 'SCENE_PORTRAIT'),
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
  const meta = STEP_BY_KEY[key]
  const doneKeys = extra.doneKeys || doneBefore(key)
  return {
    key,
    stepNo: meta?.stepNo || 1,
    stepLabel: meta?.label || '',
    tab: meta?.tab || 'script',
    doneKeys,
    allDone: doneKeys.length === VIDEO_PATH_STEPS.length,
    waiting: !!extra.waiting,
    action: extra.action || '',
    secondary: extra.secondary || null,
    kind: extra.kind || 'open',
    title: episodeTitle(input.episodeNo, extra.title),
    detail: extra.detail || '',
    look: extra.look || null,
  }
}

export function resolveViewingStageKey({ activeTab, requestedStage, currentStep, outlineCount = 0 } = {}) {
  if (AUX_STUDIO_TOOLS.some((item) => item.key === activeTab)) return null
  const requested = STEP_BY_KEY[requestedStage]
  if (requested && requested.tab === activeTab) return requested.key
  if (currentStep?.tab === activeTab) return currentStep.key
  if (activeTab === 'script') return outlineCount > 0 ? 'script' : 'outline'
  if (activeTab === 'characters') return 'looks'
  if (activeTab === 'scenes') return 'scenes'
  if (activeTab === 'storyboard') return 'shots'
  if (activeTab === 'compose') return 'film'
  return null
}

export function buildStepItems(currentStep, viewingStageKey) {
  const allDone = Boolean(currentStep?.allDone)
  const doneSet = new Set(currentStep?.doneKeys || [])
  return VIDEO_PATH_STEPS.map((item) => {
    let state = 'todo'
    let statusLabel = '未开始'
    if (allDone) {
      state = 'done'
      statusLabel = '已完成'
    } else if (item.key === currentStep?.key) {
      if (currentStep?.waiting) {
        state = 'running'
        statusLabel = '生成中'
      } else {
        state = 'current'
        statusLabel = '当前环节'
      }
    } else if (doneSet.has(item.key)) {
      state = 'done'
      statusLabel = '已完成'
    }
    return {
      ...item,
      state,
      statusLabel,
      isViewing: viewingStageKey === item.key,
    }
  })
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
    const score = resolveScriptRhythmScore(data.totalScore)
    const low = score !== null && score > 0 && score < SCRIPT_READY_MIN_SCORE
    return step(data, 'ready', {
      kind: 'ready',
      title: '把这一集剧本定稿',
      detail: low
        ? `节奏分 ${score}（未满 60 分），确认后可定稿。`
        : '定稿后自动提取角色、场景和道具。',
      action: '定稿',
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

  const sceneRefs = Array.isArray(data.sceneRefs) ? data.sceneRefs : []
  if (sceneRefs.length) {
    if (data.scenePortraitRunning) {
      return step(data, 'scenes', {
        kind: 'wait',
        waiting: true,
        title: '场景参考图正在生成',
        detail: '生成完再锁定。没锁定的场景，出图和出视频都会失败。',
      })
    }
    const first = sceneRefs[0]
    const names = sceneRefs.slice(0, 3).map((item) => item.name).join('、')
    if (first.candidateId) {
      return step(data, 'scenes', {
        kind: 'lock',
        look: first,
        title: `锁定「${first.name}」的场景参考图`,
        detail: `还没锁定的场景有 ${names}。已有图的先锁定，不用再花钱。`,
        action: `锁定「${first.name}」`,
      })
    }
    return step(data, 'scenes', {
      kind: 'portrait',
      look: first,
      title: `先给「${first.name}」出场景参考图`,
      detail: `还没锁定的场景有 ${names}。会出 2 张并锁定第一张。没锁定的场景，后面的出图和出视频都会失败。`,
      action: `给「${first.name}」出参考图`,
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
      title: '补齐镜头画面描述',
      detail: '有镜头还没有画面描述，出图会被拦住。这一步不花钱。',
      action: '补齐画面描述',
    })
  }
  if (unframed.length) {
    if (data.imageShotRunning) {
      return step(data, 'shots', {
        kind: 'wait',
        waiting: true,
        title: '首帧正在生成',
        detail: '先别重复提交。好了之后才能生成视频。',
      })
    }
    return step(data, 'shots', {
      kind: 'frame',
      title: '先出首帧，才能生成视频',
      detail: '没有首帧的镜头，生成视频会被直接拒绝。确认费用后才出图。',
      action: '出首帧',
    })
  }
  const videoCount = data.videoCount != null ? data.videoCount : shots.filter(shotHasVideo).length
  if (videoCount < data.shotCount) {
    if (data.videoShotRunning) {
      return step(data, 'shots', {
        kind: 'wait',
        waiting: true,
        title: '视频正在生成',
        detail: '生成通常要几分钟。先别重复提交。',
      })
    }
    return step(data, 'shots', {
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
