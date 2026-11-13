export const OFFICIAL_VIDEO_IN_PROGRESS = '这一镜视频已在生成中'

function extractErrorMessage(source) {
  if (!source) return ''
  if (typeof source === 'string') return source
  const responseMessage = source?.response?.data?.message
  if (responseMessage) return String(responseMessage)
  if (source?.message) return String(source.message)
  return ''
}

export function officialRunnableShots(preview) {
  const shots = preview && Array.isArray(preview.shot_list) ? preview.shot_list : []
  return {
    runnable: shots.filter((shot) => Number(shot.blocked) !== 1),
    blocked: shots.filter((shot) => Number(shot.blocked) === 1),
  }
}

/**
 * Preview, then a charge confirm, then a new official job.
 * Cancel and an in-progress shot do not submit.
 */
export async function runOfficialShotVideo({ preview, confirm, submit, regenerate = false }) {
  let viewed
  try {
    viewed = await preview()
  } catch (err) {
    return {
      submitted: false,
      reason: 'preview_failed',
      message: extractErrorMessage(err) || '视频费用预估失败，请稍后重试',
    }
  }
  if (!viewed || viewed.success === false) {
    return {
      submitted: false,
      reason: 'preview_failed',
      message: extractErrorMessage(viewed) || '视频费用预估失败，请检查前置条件',
    }
  }
  const result = viewed.result || viewed
  const { runnable, blocked } = officialRunnableShots(result)
  if (!runnable.length) {
    const reasons = [...new Set(blocked.map((item) => item && item.block_reason).filter(Boolean))]
    return {
      submitted: false,
      reason: 'in_progress',
      message: reasons.join('；') || OFFICIAL_VIDEO_IN_PROGRESS,
      blocked,
    }
  }
  const accepted = await confirm(result, { regenerate: !!regenerate })
  if (!accepted) {
    return { submitted: false, reason: 'cancelled' }
  }
  let submitted
  try {
    submitted = await submit({ confirm_cost: 1 })
  } catch (err) {
    return {
      submitted: false,
      reason: 'submit_failed',
      message: extractErrorMessage(err) || '视频任务提交失败，请稍后重试',
    }
  }
  if (!submitted || submitted.success === false) {
    return {
      submitted: false,
      reason: 'submit_failed',
      message: extractErrorMessage(submitted) || '视频任务提交失败，请稍后重试',
    }
  }
  return { submitted: true, reason: 'queued', result: submitted.result || null }
}
