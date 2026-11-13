export const OFFICIAL_VIDEO_IN_PROGRESS = '这一镜视频已在生成中'

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
  } catch {
    return { submitted: false, reason: 'preview_failed' }
  }
  if (!viewed || viewed.success === false) {
    return { submitted: false, reason: 'preview_failed' }
  }
  const result = viewed.result || viewed
  const { runnable, blocked } = officialRunnableShots(result)
  if (!runnable.length) {
    const reason = blocked[0] && blocked[0].block_reason
    return {
      submitted: false,
      reason: 'in_progress',
      message: reason || OFFICIAL_VIDEO_IN_PROGRESS,
    }
  }
  const accepted = await confirm(result, { regenerate: !!regenerate })
  if (!accepted) {
    return { submitted: false, reason: 'cancelled' }
  }
  let submitted
  try {
    submitted = await submit({ confirm_cost: 1 })
  } catch {
    return { submitted: false, reason: 'submit_failed' }
  }
  if (!submitted || submitted.success === false) {
    return { submitted: false, reason: 'submit_failed' }
  }
  return { submitted: true, reason: 'queued', result: submitted.result || null }
}
