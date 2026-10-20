/**
 * 镜头语言、运镜方式与短剧风格预设引擎 (Cinematography & Drama Style Presets)
 * 参考并融合了 ZJT (智剧通运镜库与视频后缀)、LocalMiniDrama (四宫格机位与风格定制) 与 ArcReel (镜头动作参数)。
 */

export const SHOT_TYPES = [
  { value: 'EXTREME_WIDE', label: '大远景 (EWS)', prompt: 'extreme wide establishing shot, vast epic environment view, atmospheric scale' },
  { value: 'WIDE', label: '全景 (Wide)', prompt: 'cinematic wide angle shot, full body framing, balanced environment composition' },
  { value: 'MEDIUM', label: '中景 (Medium)', prompt: 'medium shot, waist up framing, natural character interaction and posture' },
  { value: 'CLOSE_UP', label: '特写 (Close-Up)', prompt: 'dramatic close-up shot, intense facial expression, sharp focus on eyes, shallow depth of field' },
  { value: 'EXTREME_CLOSE_UP', label: '大特写 (ECU)', prompt: 'macro extreme close-up, hyper-detailed micro facial twitch, emotional tension' },
  { value: 'OVER_SHOULDER', label: '过肩镜头 (OTS)', prompt: 'over-the-shoulder perspective shot, depth staging, dynamic conversation framing' },
  { value: 'HIGH_ANGLE', label: '俯拍视角 (High Angle)', prompt: 'high angle shot looking down, vulnerable atmosphere, dramatic overhead perspective' },
  { value: 'LOW_ANGLE', label: '仰拍视角 (Low Angle)', prompt: 'heroic low angle shot looking up, dominant presence, imposing visual power' },
]

export const CAMERA_MOVEMENTS = [
  { value: 'STATIC', label: '固定机位 (Static)', prompt: 'locked-off tripod static shot, steady composition' },
  { value: 'PUSH_IN', label: '缓慢推近 (Push In)', prompt: 'slow smooth push-in camera movement, escalating narrative tension' },
  { value: 'PULL_OUT', label: '缓慢拉远 (Pull Out)', prompt: 'smooth pull-out camera movement, revealing surroundings and isolation' },
  { value: 'PAN_LEFT', label: '向左摇镜 (Pan Left)', prompt: 'fluid cinematic horizontal pan left, sweeping scene transition' },
  { value: 'PAN_RIGHT', label: '向右摇镜 (Pan Right)', prompt: 'fluid cinematic horizontal pan right, following visual flow' },
  { value: 'TILT_UP', label: '向上摇镜 (Tilt Up)', prompt: 'smooth vertical tilt-up movement, revealing subject height and scale' },
  { value: 'TILT_DOWN', label: '向下摇镜 (Tilt Down)', prompt: 'smooth vertical tilt-down movement, focusing from setting to subject' },
  { value: 'TRACKING', label: '跟拍跟随 (Tracking)', prompt: 'dynamic tracking shot, steadicam smoothly following character motion' },
  { value: 'ORBIT', label: '环绕运镜 (Orbit 360)', prompt: 'dramatic 360 degree arc orbit camera movement circling the subject' },
  { value: 'DOLLY_ZOOM', label: '希区柯克变焦 (Dolly Zoom)', prompt: 'vertigo effect dolly zoom, background warping while subject size remains stable' },
]

export const DRAMA_STYLES = [
  {
    key: 'URBAN_REVENGE',
    label: '都市逆袭 / 豪门爽剧',
    keywords: 'modern high-end luxury aesthetic, sharp contrast, cinematic rim lighting, 8k uhd, photorealistic, elegant business attire',
  },
  {
    key: 'XIANXIA_FANTASY',
    label: '国风修仙 / 东方玄幻',
    keywords: 'ethereal chinese fantasy, flowing ancient silk robes, floating celestial energy, mist and spiritual glow, mythical lighting, 8k',
  },
  {
    key: 'MYSTERY_NOIR',
    label: '悬疑暗黑 / 罪案反转',
    keywords: 'cinematic noir, moody dramatic shadows, volumetric fog, rainy neon reflections, high tension atmosphere, desaturated filmic look',
  },
  {
    key: 'SWEET_ROMANCE',
    label: '现代甜宠 / 唯美纯爱',
    keywords: 'warm golden hour lighting, soft dreamy bokeh, gentle sunlight flare, delicate romantic atmosphere, pastel warm tones, soft focus',
  },
  {
    key: 'ANIME_COMIC',
    label: '热血漫剧 / 二次元',
    keywords: 'high-contrast dynamic anime comic style, clean line art, vibrant comic book lighting, dramatic cel shading, 2.5D animation aesthetic',
  },
  {
    key: 'VINTAGE_FILM',
    label: '复古胶片 / 港风经典',
    keywords: '35mm film grain, vintage color grading, nostalgic warm glow, classic analog cinema texture, Kodak Portra tone',
  },
]

export const QUALITY_MODIFIERS = [
  'cinematic lighting',
  '8k uhd',
  'hyperrealistic',
  'volumetric light',
  'shot on 35mm lens',
  'masterpiece',
  'photorealistic textures',
  'award winning cinematography',
]

export const QUAD_GRID_ANGLES = [
  { value: 'FRONT', label: '正面平视 (Front)', prompt: 'eye-level front shot, clear centered face, symmetrical framing' },
  { value: 'THREE_QUARTER', label: '3/4侧面视角 (3/4 Profile)', prompt: 'cinematic 45-degree three-quarter angle, depth of field' },
  { value: 'OVER_SHOULDER', label: '过肩对峙 (OTS)', prompt: 'over-the-shoulder conversation confrontation, subtle foreground shoulder blur' },
  { value: 'CLOSE_UP', label: '微距特写 (Macro Detail)', prompt: 'extreme close-up on eyes, emotional intensity, high dynamic range' },
]

export const CONTINUITY_PRESETS = [
  { value: 'MATCH_CUT', label: '同场次首尾帧顺接', prompt: 'seamless match cut, continuity with previous shot end frame, consistent lighting and character position' },
  { value: 'HARD_CUT', label: '跨场景硬切', prompt: 'hard cut to new location, scene transition, establishing environment' },
]

export const ASPECT_RATIOS = [
  { value: '9:16', label: '竖屏短剧 (9:16 · 含底部/右侧字幕安全区)', safeBottomPct: 18, safeRightPct: 14 },
  { value: '16:9', label: '横屏剧集 (16:9 · 标准宽银幕安全区)', safeBottomPct: 10, safeRightPct: 8 },
]

export const RHYTHM_MARKERS = [
  { code: 'HOOK_3S', key: 'HOOK_3S', label: '黄金3秒钩子', targetSec: 3, desc: '开篇3秒内抛出强冲突或悬念' },
  { code: 'MID_REVERSAL', key: 'MID_REVERSAL', label: '中段高潮反转', targetSec: 35, desc: '剧集中段关系或局势逆转' },
  { code: 'CLIFFHANGER_END', key: 'CLIFFHANGER_END', label: '集尾悬念卡点', targetSec: 85, desc: '集尾留白或疑问引导下一集' },
]

/**
 * 组装增强后的镜头视觉提示词
 */
export function buildEnhancedPrompt(basePrompt = '', options = {}) {
  const parts = [basePrompt.trim()]

  if (options.shotType) {
    const st = SHOT_TYPES.find((item) => item.value === options.shotType)
    if (st) parts.push(st.prompt)
  }

  if (options.cameraMovement && options.cameraMovement !== 'STATIC') {
    const cm = CAMERA_MOVEMENTS.find((item) => item.value === options.cameraMovement)
    if (cm) parts.push(cm.prompt)
  }

  if (options.styleKey) {
    const style = DRAMA_STYLES.find((item) => item.key === options.styleKey)
    if (style) parts.push(style.keywords)
  }

  if (options.qualityBoost) {
    parts.push('masterpiece, 8k uhd, cinematic lighting, photorealistic')
  }

  return parts.filter(Boolean).join(', ')
}

/**
 * 对标 Jellyfish & waoowaoo：生成前统一镜头就绪度与资产锚点锁定预检
 */
export function evaluateShotReadiness(shot = {}, consistencyPanel = {}) {
  const blockers = []
  const warnings = []
  const promptText = String(shot.previz_text || shot.image_prompt || shot.shot_desc || '').trim()
  if (!promptText) {
    blockers.push('缺少镜头画面描述或人话分镜')
  } else if (promptText.length < 6) {
    warnings.push('镜头描述较短，建议补充景别、运镜或动作细节')
  }

  const charList = consistencyPanel.character_list || []
  const sceneList = consistencyPanel.scene_list || []
  const propList = consistencyPanel.prop_list || []

  for (const charId of shot.character_ref_json || []) {
    const card = charList.find((c) => c.character_id === charId)
    if (card && card.card_status !== 'LOCKED' && card.lock_flag !== 1) {
      blockers.push(`关联角色「${card.character_name || charId}」尚未锁定定妆`)
    }
  }
  if (shot.scene_card_id) {
    const sceneCard = sceneList.find((s) => s.scene_card_id === shot.scene_card_id)
    if (sceneCard && sceneCard.card_status !== 'LOCKED' && sceneCard.lock_flag !== 1) {
      blockers.push(`关联场景「${sceneCard.card_name || shot.scene_card_id}」尚未锁定参考图`)
    }
  }
  for (const propId of shot.prop_ref_json || []) {
    const propCard = propList.find((p) => p.scene_card_id === propId)
    if (propCard && propCard.card_status !== 'LOCKED' && propCard.lock_flag !== 1) {
      blockers.push(`关联道具「${propCard.card_name || propId}」尚未锁定参考图`)
    }
  }

  if (shot.stale_flag === 1) {
    warnings.push('关联资产定妆已变更，建议重抽以保持跨镜一致')
  }
  if (shot.blocked_flag === 1) {
    blockers.push('镜头处于阻塞状态，请先解除前置依赖')
  }

  const hasVideo = Boolean(shot.video_asset_id || shot.shot_status === 'VIDEO_READY' || shot.shot_status === 'LOCKED')
  const hasFirstFrame = Boolean(shot.first_frame_asset_id || shot.first_frame_url)
  let stage = 'DRAFT'
  if (hasVideo) {
    stage = 'VIDEO_READY'
  } else if (hasFirstFrame) {
    stage = 'IMAGE_READY'
  } else if (promptText.length >= 6) {
    stage = 'PROMPT_READY'
  }

  return {
    ready: blockers.length === 0,
    canGenerateVideo: blockers.length === 0 && hasFirstFrame,
    stage,
    blockers,
    warnings,
  }
}

/**
 * 对标 ArcReel：发布到 bgssai-tube-pc 前的成片交付前置校验
 */
export function validateTubePublishPreflight(film = {}, episode = {}, preflight = {}) {
  const blockers = []
  if (!film || !film.film_id) {
    blockers.push('尚未生成成片记录')
  } else {
    if (film.compose_status !== 'READY') {
      blockers.push(`成片状态未就绪（当前为 ${film.compose_status || 'NONE'}，需为已就绪）`)
    }
    if (film.expired_flag === 1) {
      blockers.push('成片已因镜头或配音变更而过期，请重新合成')
    }
    const durationSec = Number(film.film_duration_sec ?? 1)
    if (!Number.isFinite(durationSec) || durationSec <= 0) {
      blockers.push('成片缺少有效正数时长（duration_sec 须大于 0）')
    }
    const url = String(film.download_url || '').trim()
    if (url && (url.startsWith('stub://') || url.startsWith('placeholder://'))) {
      blockers.push('成片地址为占位地址，禁止交付到 Tube')
    }
  }
  const reviewStatus = episode?.review_status || preflight?.review_status
  if (reviewStatus && reviewStatus !== 'APPROVED') {
    blockers.push(`剧集审核未通过（当前为 ${reviewStatus}，需为已通过）`)
  }
  if (preflight && preflight.config_ready === false) {
    blockers.push('Tube 发布配置未就绪（需配置主机与摄入令牌）')
  }
  const workId = film?.work_id || preflight?.work_id || ''
  const episodeId = film?.episode_id || episode?.episode_id || preflight?.episode_id || ''
  const filmId = film?.film_id || preflight?.film_id || ''
  const idempotencyKey = workId && episodeId && filmId ? `short:${workId}:${episodeId}:${filmId}` : ''

  return {
    ready: blockers.length === 0,
    publishable: blockers.length === 0,
    idempotencyKey,
    endpoint: '/bgssai/user/tube/ingest/short',
    blockers,
  }
}


