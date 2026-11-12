export const CREATE_WORK_DEFAULTS = {
  genre: 'REVENGE',
  visual_style: 'MODERN_URBAN',
  episode_count: 3,
  episode_duration_sec: 60,
}

const GENRE_NAME = {
  REVENGE: '复仇逆袭',
  CEO: '豪门总裁',
  TIME_TRAVEL: '穿越重生',
  SWEET: '甜宠',
  SUSPENSE: '悬疑',
  FANTASY: '玄幻',
}

const STYLE_NAME = {
  MODERN_URBAN: '都市写实',
  ANCIENT_CHINESE: '古装',
  ANIME: '动漫',
  CINEMATIC: '电影感',
}

export function createWorkSettingLine(values = {}) {
  const genre = GENRE_NAME[values.genre] || GENRE_NAME[CREATE_WORK_DEFAULTS.genre]
  const style = STYLE_NAME[values.visual_style] || STYLE_NAME[CREATE_WORK_DEFAULTS.visual_style]
  const count = Number(values.episode_count) || CREATE_WORK_DEFAULTS.episode_count
  const seconds = Number(values.episode_duration_sec) || CREATE_WORK_DEFAULTS.episode_duration_sec
  return `当前会按「${genre} · ${count} 集 × ${seconds} 秒 · ${style}」创建。这是先试做的短设置。想做完整的 24 集 × 90 秒，打开高级设置再改。`
}
