const ALIAS = {
  'bgssai-chat': 'BGSSAI Chat',
  'bgssai-image': 'BGSSAI Image',
  'bgssai-video': 'BGSSAI Video',
  'bgssai-tts': 'BGSSAI TTS',
  'bgssai-asr': 'BGSSAI ASR',
  'bgssai-music': 'BGSSAI Music',
}

const CODE_BY_ABILITY = {
  TEXT: 'bgssai-chat',
  IMAGE: 'bgssai-image',
  VIDEO: 'bgssai-video',
  VOICE: 'bgssai-tts',
  ASR: 'bgssai-asr',
  MUSIC: 'bgssai-music',
}

const VENDOR = /stable[\s-]?diffusion|sd-turbo|sdxl|cogvideox|cogvideo|qwen|deepseek|minimax|海螺|kling|可灵|pixverse|hunyuan|混元|openai|chatgpt|gpt-|claude|anthropic|gemini|midjourney|whisper|piper|edge-tts|cosyvoice|dashscope|moonshot|\bkimi\b|vidu|seedance|token\s*-?\s*hub|tencentmaas|\bflux\b|\bsora\b|runway|\bluma\b|\bpika\b|\bsuno\b|\budio\b|即梦|豆包|zhipu|chatglm|hailuo|wanx|kolors|通义|文心|hy-asr|hy-vision|hy[0-9]|speech-[0-9]|image-01/i

export function publicModelCode(value, ability) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const key = raw.toLowerCase()
  if (ALIAS[key]) return key
  if (VENDOR.test(raw)) return CODE_BY_ABILITY[String(ability || '').toUpperCase()] || 'bgssai'
  return raw
}

export function publicModelText(value, ability) {
  const raw = String(value || '').trim()
  if (!raw) return ''
  const key = raw.toLowerCase()
  if (ALIAS[key]) return ALIAS[key]
  if (VENDOR.test(raw)) {
    const code = CODE_BY_ABILITY[String(ability || '').toUpperCase()]
    if (code && raw.length < 80) return ALIAS[code]
    const cleaned = scrubVendorText(raw)
    return cleaned || (code ? ALIAS[code] : 'BGSSAI')
  }
  return raw
}

export function scrubVendorText(text) {
  let msg = String(text || '')
  if (!msg) return ''
  msg = msg.replace(new RegExp(VENDOR.source, 'ig'), '')
  msg = msg.replace(/token\s*-?\s*hub/ig, '')
  msg = msg.replace(/：\s*，/g, '，').replace(/：\s*。/g, '。').replace(/（\s*）/g, '')
  return msg.replace(/[ \t]{2,}/g, ' ').trim()
}
