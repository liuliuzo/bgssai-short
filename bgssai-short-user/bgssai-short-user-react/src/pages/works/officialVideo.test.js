import test from 'node:test'
import assert from 'node:assert/strict'
import { officialRunnableShots, runOfficialShotVideo } from './officialVideo.js'

test('cancel does not submit and an in-progress shot does not open a charge', async () => {
  let submitted = 0
  let confirmed = 0
  const cancelled = await runOfficialShotVideo({
    preview: async () => ({
      success: true,
      result: {
        estimated_cost_cent: 80,
        shot_list: [{ shot_id: 's-1', shot_no: 1, blocked: 0, route: 'IMAGE_TO_VIDEO', size_note: '384×256' }],
      },
    }),
    confirm: async () => {
      confirmed += 1
      return false
    },
    submit: async () => {
      submitted += 1
      return { success: true }
    },
    regenerate: true,
  })
  assert.equal(cancelled.reason, 'cancelled')
  assert.equal(cancelled.submitted, false)
  assert.equal(confirmed, 1)
  assert.equal(submitted, 0)

  const busy = await runOfficialShotVideo({
    preview: async () => ({
      success: true,
      result: {
        shot_list: [{ shot_id: 's-1', blocked: 1, block_reason: '这一镜视频已在生成中' }],
      },
    }),
    confirm: async () => {
      confirmed += 1
      return true
    },
    submit: async () => {
      submitted += 1
      return { success: true }
    },
  })
  assert.equal(busy.reason, 'in_progress')
  assert.equal(busy.submitted, false)
  assert.equal(confirmed, 1)
  assert.equal(submitted, 0)
  assert.equal(officialRunnableShots({ shot_list: [{ blocked: 1 }] }).runnable.length, 0)
})

test('a confirmed regenerate submits once and does not resume', async () => {
  const bodies = []
  const outcome = await runOfficialShotVideo({
    regenerate: true,
    preview: async () => ({ success: true, result: { shot_list: [{ shot_id: 's-1', blocked: 0 }] } }),
    confirm: async () => true,
    submit: async (body) => {
      bodies.push(body)
      return { success: true, result: { batch_id: 'b-1', resumed_existing_video: false } }
    },
  })
  assert.equal(outcome.submitted, true)
  assert.deepEqual(bodies, [{ confirm_cost: 1 }])
  assert.equal(outcome.result.resumed_existing_video, false)
})

test('preview and submit errors return readable failure messages', async () => {
  const previewErr = await runOfficialShotVideo({
    preview: async () => ({ success: false, message: '可用额度不足，请先充值加量包' }),
    confirm: async () => true,
    submit: async () => ({ success: true }),
  })
  assert.equal(previewErr.submitted, false)
  assert.equal(previewErr.reason, 'preview_failed')
  assert.match(previewErr.message, /额度不足/)

  const submitErr = await runOfficialShotVideo({
    preview: async () => ({ success: true, result: { shot_list: [{ shot_id: 's-1', blocked: 0 }] } }),
    confirm: async () => true,
    submit: async () => {
      const err = new Error('network')
      err.response = { data: { message: '镜1 引用的角色「沈知微」尚未定妆，请先完成定妆' } }
      throw err
    },
  })
  assert.equal(submitErr.submitted, false)
  assert.equal(submitErr.reason, 'submit_failed')
  assert.match(submitErr.message, /沈知微/)
})
