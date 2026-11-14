import { isPlaceholderMediaUrl, isRealPhotoUrl } from '../../lib/mediaUrl.js'

export function hasPlayableShotVideo(shot) {
  if (!shot || !shot.video_url) return false
  if (Number(shot.video_placeholder_flag) === 1) return false
  return !isPlaceholderMediaUrl(shot.video_url)
}

export function hasRealFirstFrame(shot) {
  if (!shot || !shot.first_frame_url) return false
  if (Number(shot.first_frame_placeholder_flag) === 1) return false
  return !isPlaceholderMediaUrl(shot.first_frame_url)
}

export function isShotVideoActive(shot) {
  if (!shot) return false
  if (shot.shot_status === 'VIDEO_GENERATING') return true
  const latest = shot.latest_task
  if (!latest || latest.task_type !== 'VIDEO') return false
  return latest.task_status === 'QUEUED' || latest.task_status === 'RUNNING'
}

export function summarizeConsistencyCards(panel) {
  const chars = Array.isArray(panel?.character_list) ? panel.character_list : []
  const scenes = Array.isArray(panel?.scene_list) ? panel.scene_list : []
  const props = Array.isArray(panel?.prop_list) ? panel.prop_list : []
  const items = [
    ...chars.map((c) => ({
      kind: '角色',
      id: c.character_id,
      name: c.character_name || '未命名角色',
      status: c.card_status || 'CANDIDATE',
      locked: c.card_status === 'LOCKED' && Boolean(c.ref_image_asset_id || isRealPhotoUrl(c.ref_image_url)),
      hasCandidatePhoto: Array.isArray(c.candidate_list) && c.candidate_list.some((item) => isRealPhotoUrl(item?.file_url)),
      usedShotCount: Number(c.used_shot_count) || 0,
    })),
    ...scenes.map((c) => ({
      kind: '场景',
      id: c.scene_card_id,
      name: c.card_name || '未命名场景',
      status: c.card_status || 'CANDIDATE',
      locked: c.card_status === 'LOCKED' && Boolean(c.ref_image_asset_id || isRealPhotoUrl(c.ref_image_url)),
      hasCandidatePhoto: Array.isArray(c.candidate_list) && c.candidate_list.some((item) => isRealPhotoUrl(item?.file_url)),
      usedShotCount: Number(c.used_shot_count) || 0,
    })),
    ...props.map((c) => ({
      kind: '道具',
      id: c.scene_card_id,
      name: c.card_name || '未命名道具',
      status: c.card_status || 'CANDIDATE',
      locked: c.card_status === 'LOCKED' && Boolean(c.ref_image_asset_id || isRealPhotoUrl(c.ref_image_url)),
      hasCandidatePhoto: Array.isArray(c.candidate_list) && c.candidate_list.some((item) => isRealPhotoUrl(item?.file_url)),
      usedShotCount: Number(c.used_shot_count) || 0,
    })),
  ]
  const unlockedCards = items.filter((item) => !item.locked)
  const lockedCards = items.filter((item) => item.locked)
  return {
    totalCards: items.length,
    lockedCount: lockedCards.length,
    unlockedCards,
    pendingSelectionCards: unlockedCards.filter((item) => item.hasCandidatePhoto),
    unlockedUsedCards: unlockedCards.filter((item) => item.usedShotCount > 0),
    unlockedRefCount: Number(panel?.unlocked_ref_count) || 0,
  }
}

export function selectPendingVideoShotIds(shots) {
  if (!Array.isArray(shots)) return []
  return shots
    .filter((shot) => shot && shot.shot_id)
    .filter((shot) => shot.shot_status !== 'LOCKED')
    .filter((shot) => Number(shot.blocked_flag) !== 1)
    .filter((shot) => !hasPlayableShotVideo(shot))
    .filter((shot) => !isShotVideoActive(shot))
    .map((shot) => shot.shot_id)
}

export function resolveRepairAction(errorMessage) {
  const msg = String(errorMessage || '')
  if (/定妆|锁定|角色|场景|道具|占位图/.test(msg)) {
    return { label: '去角色定妆处理', tab: 'characters' }
  }
  if (/余额|额度|套餐|加量|触顶|充值/.test(msg)) {
    return { label: '去套餐与加量', href: '/subscription' }
  }
  if (/定稿|剧本/.test(msg)) {
    return { label: '去剧本与定稿', tab: 'script' }
  }
  return { label: '打开任务中心', href: '/tasks' }
}

/** Script tab copy once the episode is already finalized and has a film. */
export function scriptStudioGuide({ scriptStatus, composeStatus } = {}) {
  const ready = scriptStatus === 'READY' || scriptStatus === 'LOCKED'
  const filmed = composeStatus === 'READY'
  if (ready && filmed) {
    return {
      mode: 'filmed',
      title: '剧本已定稿，这一集已有成片',
      detail: '生成剧本、定稿和强制定稿不是当前要做的事。要看或下载成片，去合成页。',
      offerGenerate: false,
    }
  }
  return {
    mode: 'editing',
    title: '第 1 步：剧本与定稿',
    detail: '先确认全集大纲，再生成当前这一集的分场剧本并点击「定稿剧本」。定稿后会自动提取角色和场景，进入下一步。',
    offerGenerate: true,
  }
}

export function buildProductionFlow({
  episode = null,
  outlineCount = 0,
  scriptSceneCount = 0,
  consistency = null,
  shots = [],
  composeEntry = null,
} = {}) {
  const safeShots = Array.isArray(shots) ? shots : []
  const scriptStatus = episode?.script_status || 'NONE'
  const scriptReady = scriptStatus === 'READY'
  const scriptGenerating = scriptStatus === 'GENERATING'
  const hasScriptDraft = scriptReady || scriptStatus === 'DRAFT' || scriptSceneCount > 0

  const cardSummary = summarizeConsistencyCards(consistency)
  const blockedShots = safeShots.filter((shot) => Number(shot?.blocked_flag) === 1)
  const videoReadyCount = safeShots.filter(hasPlayableShotVideo).length
  const firstFrameCount = safeShots.filter(hasRealFirstFrame).length
  const videoGeneratingCount = safeShots.filter(isShotVideoActive).length
  const totalShots = safeShots.length
  const allShotVideosReady = totalShots > 0 && videoReadyCount === totalShots

  const film = composeEntry?.film || null
  const filmExpired = Number(film?.expired_flag) === 1
  const filmComposing = film?.compose_status === 'COMPOSING' || episode?.compose_status === 'COMPOSING'
  const filmReady = !filmExpired && (film?.compose_status === 'READY' || (!film && episode?.compose_status === 'READY'))
  const dubbingReady = episode?.dubbing_status === 'READY'

  const looksBlocked = blockedShots.length > 0 || cardSummary.unlockedRefCount > 0
  const looksNeedInitialLock = scriptReady && totalShots === 0 && cardSummary.unlockedCards.length > 0
  const looksDone = scriptReady && !looksBlocked && !looksNeedInitialLock

  const step1 = {
    stepNo: 1,
    key: 'script',
    tab: 'script',
    title: '1. 剧本与定稿',
    status: scriptReady ? 'done' : (scriptGenerating || hasScriptDraft || outlineCount > 0 ? 'doing' : 'todo'),
    statusText: scriptReady
      ? '已定稿'
      : scriptGenerating
        ? '生成中'
        : hasScriptDraft
          ? '待定稿'
          : outlineCount > 0
            ? '待生剧本'
            : '待生大纲',
    summary: scriptReady
      ? '剧本已定稿'
      : hasScriptDraft
        ? '请点击定稿剧本'
        : outlineCount > 0
          ? '大纲已出，待生成剧本'
          : '先生成全集大纲',
  }

  const step2 = {
    stepNo: 2,
    key: 'characters',
    tab: 'characters',
    title: '2. 角色定妆',
    status: !scriptReady
      ? 'todo'
      : looksBlocked
        ? 'blocked'
        : looksNeedInitialLock
          ? 'doing'
          : 'done',
    statusText: !scriptReady
      ? '待剧本定稿'
      : looksBlocked || looksNeedInitialLock
        ? `${cardSummary.unlockedCards.length || blockedShots.length} 待锁定`
        : cardSummary.totalCards > 0
          ? `${cardSummary.lockedCount}/${cardSummary.totalCards} 已锁定`
          : '无需定妆',
    summary: looksBlocked
      ? `有 ${blockedShots.length || cardSummary.unlockedRefCount} 镜待锁定定妆`
      : looksNeedInitialLock
        ? `${cardSummary.unlockedCards.length} 张卡片待选图锁定`
        : cardSummary.totalCards > 0
          ? `已锁定 ${cardSummary.lockedCount} 张资产卡`
          : '定稿后自动提取角色与场景',
  }

  const step3 = {
    stepNo: 3,
    key: 'storyboard',
    tab: 'storyboard',
    title: '3. 分镜与视频',
    status: allShotVideosReady
      ? 'done'
      : totalShots > 0 || videoGeneratingCount > 0
        ? (looksBlocked ? 'blocked' : 'doing')
        : 'todo',
    statusText: allShotVideosReady
      ? `${videoReadyCount}/${totalShots} 视频就绪`
      : totalShots > 0
        ? `视频 ${videoReadyCount}/${totalShots}`
        : '待拆镜',
    summary: totalShots > 0
      ? `共 ${totalShots} 镜 · 首帧 ${firstFrameCount} · 视频 ${videoReadyCount}`
      : '从定稿剧本一键拆镜',
  }

  const step4 = {
    stepNo: 4,
    key: 'voice',
    tab: 'voice',
    title: '4. 配音字幕（可选）',
    status: dubbingReady ? 'done' : 'optional',
    statusText: dubbingReady ? '配音就绪' : '可选步骤',
    summary: dubbingReady ? '已生成本集配音' : '不配音也可直接带字幕合成',
  }

  const step5 = {
    stepNo: 5,
    key: 'compose',
    tab: 'compose',
    title: '5. 合成成片',
    status: filmReady ? 'done' : filmComposing || allShotVideosReady ? 'doing' : 'todo',
    statusText: filmReady ? '成片就绪' : filmComposing ? '合成中' : allShotVideosReady ? '可合成' : '待视频齐备',
    summary: filmReady ? '可预览下载与提审' : allShotVideosReady ? '镜头视频已齐，可一键合成' : '全部镜头有视频后可合成',
  }

  let nextAction
  if (scriptGenerating) {
    nextAction = {
      stepNo: 1,
      tone: 'info',
      title: '第 1 步：正在生成本集剧本',
      description: '系统正在编写本集分场台词，生成完成后点击「定稿剧本」即可自动提取角色进入定妆。',
      primaryLabel: '查看剧本进度',
      primaryTab: 'script',
    }
  } else if (!scriptReady && outlineCount === 0 && !hasScriptDraft) {
    nextAction = {
      stepNo: 1,
      tone: 'info',
      title: '第 1 步：先生成全集大纲与本集剧本',
      description: '新建作品会自动生成大纲；若尚未生成，请在「剧本与定稿」页点击「生成大纲」，随后点「生成剧本」。',
      primaryLabel: '去剧本与定稿',
      primaryTab: 'script',
    }
  } else if (!scriptReady && !hasScriptDraft) {
    nextAction = {
      stepNo: 1,
      tone: 'info',
      title: '第 1 步：大纲已就绪，请生成本集剧本',
      description: '进入「剧本与定稿」页点击「生成剧本」，生成本集分场对白后再点「定稿剧本」。',
      primaryLabel: '去生成本集剧本',
      primaryTab: 'script',
    }
  } else if (!scriptReady) {
    nextAction = {
      stepNo: 1,
      tone: 'warning',
      title: '第 1 步：剧本草稿已生成，请点击「定稿剧本」',
      description: '定稿后系统会自动提取出场角色与场景卡，并解锁分镜拆解。若评分未满 60 分，也可直接点「强制定稿」继续。',
      primaryLabel: '去完成剧本定稿',
      primaryTab: 'script',
    }
  } else if (looksNeedInitialLock) {
    const names = cardSummary.unlockedCards.slice(0, 4).map((c) => `${c.kind}「${c.name}」`).join('、')
    const hasCandidates = cardSummary.pendingSelectionCards.length > 0
    nextAction = {
      stepNo: 2,
      tone: hasCandidates ? 'warning' : 'info',
      title: hasCandidates
        ? '第 2 步：候选图已生成，请点击「选定」锁定定妆'
        : `第 2 步：请先为 ${cardSummary.unlockedCards.length} 张角色/场景卡锁定参考图`,
      description: hasCandidates
        ? `待锁定：${names}。生成参考图后必须点击某张图下方的「选定」变为「已锁定」，后续分镜才能生成画面和视频。`
        : `待定妆：${names}。可生成参考图后点「选定」，或直接上传照片、从定妆库或场景库选用。`,
      primaryLabel: hasCandidates ? '去选定并锁定定妆' : '去完成角色定妆',
      primaryTab: 'characters',
      secondaryLabel: '先去拆解分镜',
      secondaryTab: 'storyboard',
    }
  } else if (totalShots === 0) {
    nextAction = {
      stepNo: 3,
      tone: 'info',
      title: '第 3 步：从定稿剧本一键拆解分镜',
      description: '剧本与角色定妆已就绪，进入「分镜与视频」点击「一键从剧本拆解分镜」即可生成镜头表。',
      primaryLabel: '去拆解分镜',
      primaryTab: 'storyboard',
    }
  } else if (looksBlocked) {
    const sampleReason = blockedShots.map((s) => s.block_reason).find(Boolean)
    nextAction = {
      stepNo: 2,
      tone: 'warning',
      title: `第 2 步：有 ${blockedShots.length || cardSummary.unlockedRefCount} 个镜头因未锁定定妆暂时无法生成`,
      description: sampleReason || '请前往「角色定妆」页为引用的角色、场景或道具点击「选定」锁定参考图，锁定后分镜会自动解锁。',
      primaryLabel: '去角色定妆解锁',
      primaryTab: 'characters',
      secondaryLabel: '查看分镜列表',
      secondaryTab: 'storyboard',
    }
  } else if (videoGeneratingCount > 0) {
    nextAction = {
      stepNo: 3,
      tone: 'info',
      title: `第 3 步：镜头视频生成中（已就绪 ${videoReadyCount}/${totalShots} 镜，进行中 ${videoGeneratingCount} 镜）`,
      description: '视频生成通常需要数分钟至十余分钟，完成后可直接在分镜卡片左侧播放，全部就绪后即可一键合成成片。',
      primaryLabel: '查看分镜视频进度',
      primaryTab: 'storyboard',
      secondaryLabel: '打开任务进度',
      secondaryTab: 'tasks',
    }
  } else if (!allShotVideosReady) {
    const remaining = totalShots - videoReadyCount
    nextAction = {
      stepNo: 3,
      tone: 'info',
      title: `第 3 步：生成本集镜头视频（还差 ${remaining} 镜，已完成 ${videoReadyCount}/${totalShots} 镜）`,
      description: '在「分镜与视频」页可先点「出草图确认构图」，或直接点「一键生成待出视频镜头」批量产出视频。',
      primaryLabel: '去分镜生成视频',
      primaryTab: 'storyboard',
    }
  } else if (filmComposing) {
    nextAction = {
      stepNo: 5,
      tone: 'info',
      title: '第 5 步：正在合成整集成片',
      description: '系统正在拼接本集全部镜头视频并烧录台词字幕，请稍候查看成片预览。',
      primaryLabel: '查看合成进度',
      primaryTab: 'compose',
    }
  } else if (!filmReady) {
    nextAction = {
      stepNo: 5,
      tone: 'success',
      title: `第 5 步：本集 ${totalShots} 个镜头视频已全部就绪，可以合成成片`,
      description: filmExpired
        ? '镜头或配音更新后旧成片已过期，请进入「合成成片」重新合成最新 MP4 成片。'
        : '可直接进入「合成成片」一键拼接整集视频并烧录字幕；如需人声配音也可先去「配音字幕（可选）」。',
      primaryLabel: filmExpired ? '去重新合成成片' : '去合成成片',
      primaryTab: 'compose',
      secondaryLabel: '去配音字幕（可选）',
      secondaryTab: 'voice',
    }
  } else {
    nextAction = {
      stepNo: 5,
      tone: 'success',
      title: '本集成片已生成完毕',
      description: '可在「合成成片」页直接在线播放、下载 MP4 视频，或提交审核与发布。',
      primaryLabel: '查看与下载成片',
      primaryTab: 'compose',
      secondaryLabel: '导出剪辑工程',
      secondaryTab: 'edit-export',
    }
  }

  return {
    steps: [step1, step2, step3, step4, step5],
    nextAction,
    metrics: {
      totalShots,
      firstFrameCount,
      videoReadyCount,
      videoGeneratingCount,
      blockedShotCount: blockedShots.length,
      pendingVideoShotIds: selectPendingVideoShotIds(safeShots),
      allShotVideosReady,
      filmReady,
      cardSummary,
    },
  }
}
