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
