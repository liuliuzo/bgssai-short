const FRAMES = new Set([9, 17, 25, 33])

export function newClientRequestId() {
  const chars = 'abcdefghijklmnopqrstuvwxyz0123456789'
  let id = 'req'
  while (id.length < 16) {
    id += chars[Math.floor(Math.random() * chars.length)]
  }
  return id
}

export function pollDelayMs(attempt) {
  const delays = [5000, 7000, 10000, 12000, 15000]
  const index = Math.max(0, Number(attempt) || 0)
  return index >= delays.length ? 15000 : delays[index]
}

function aligned(value, min, max) {
  return Number.isInteger(value) && value >= min && value <= max && value % 32 === 0
}

export function buildImageToVideoSubmit(input) {
  const source = input || {}
  const prompt = String(source.prompt || '').trim()
  if (!prompt || prompt.length > 2000) {
    return { error: '提示词需要 1 到 2000 个字符。' }
  }
  const assetId = String(source.assetId || '').trim()
  const shotId = String(source.shotId || '').trim()
  if (!assetId && !shotId) {
    return { error: '请上传或选择一张图片。' }
  }
  const clientRequestId = String(source.clientRequestId || '').trim()
  if (!/^[A-Za-z0-9_-]{8,64}$/.test(clientRequestId)) {
    return { error: '请提供 8 到 64 位的请求号。' }
  }
  const body = {
    prompt,
    client_request_id: clientRequestId,
  }
  if (source.workId) body.work_id = source.workId
  if (source.episodeId) body.episode_id = source.episodeId
  if (assetId) body.asset_id = assetId
  if (shotId) body.shot_id = shotId
  const checks = [
    ['width', source.width, (value) => aligned(value, 128, 384)],
    ['height', source.height, (value) => aligned(value, 128, 256)],
    ['num_frames', source.numFrames, (value) => FRAMES.has(value)],
    ['fps', source.fps, (value) => Number.isInteger(value) && value >= 1 && value <= 24],
    ['steps', source.steps, (value) => Number.isInteger(value) && value >= 10 && value <= 30],
  ]
  for (const [key, raw, ok] of checks) {
    if (raw === '' || raw == null) continue
    const value = Number(raw)
    if (!ok(value)) {
      return { error: '图生视频只接受宽 128-384、高 128-256（32 的倍数），帧数 9/17/25/33，帧率 1-24，步数 10-30。' }
    }
    body[key] = value
  }
  if (source.seed !== '' && source.seed != null) {
    const seed = Number(source.seed)
    if (!Number.isInteger(seed)) return { error: '种子需要是整数。' }
    body.seed = seed
  }
  return { body }
}

export function imageToVideoUnavailable(capability) {
  return !capability || Number(capability.available) !== 1
}

export const NEW_TASK_WARNING = '图生视频没有幂等键。新建任务会再提交一次，不会取消上一笔，也不会改走文生视频。'

export const LONG_RUN_HINT = '同类成片大约要二十多分钟。页面会继续查询。中断后请等待，不要再次提交。'
