import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { publicModelCode, publicModelText, scrubVendorText } from './publicModel.js'
import {
  LONG_RUN_HINT,
  NEW_TASK_WARNING,
  buildImageToVideoSubmit,
  imageToVideoUnavailable,
  pollDelayMs,
} from './imageToVideo.js'

describe('image to video client', () => {
  it('rejects out of range targets instead of clamping them', () => {
    const rejected = buildImageToVideoSubmit({
      prompt: 'a person turns',
      assetId: 'a-1',
      clientRequestId: 'req12345678',
      width: 1920,
      height: 1080,
    })
    assert.equal(rejected.body, undefined)
    assert.match(rejected.error, /128-384/)
    const accepted = buildImageToVideoSubmit({
      prompt: 'a person turns',
      shotId: 's-1',
      workId: 'w-1',
      clientRequestId: 'req12345678',
    })
    assert.equal(accepted.body.shot_id, 's-1')
    assert.equal(accepted.body.width, undefined)
    assert.equal(accepted.body.asset_id, undefined)
  })

  it('keeps the product alias and does not resubmit on its own', () => {
    assert.equal(publicModelCode('bgssai-video-i2v', 'VIDEO'), 'bgssai-video')
    assert.equal(publicModelText('bgssai-video-i2v', 'VIDEO'), 'BGSSAI Video')
    assert.equal(scrubVendorText('失败 ltx-video'), '失败')
    assert.equal(imageToVideoUnavailable({ available: 0, reason: '图生视频暂不可用。文生视频不受影响。' }), true)
    assert.equal(imageToVideoUnavailable({ available: 1 }), false)
    assert.deepEqual([0, 1, 2, 3, 4, 8].map(pollDelayMs), [5000, 7000, 10000, 12000, 15000, 15000])
    assert.match(NEW_TASK_WARNING, /没有幂等键/)
    assert.equal(NEW_TASK_WARNING.toLowerCase().includes('ltx'), false)
    assert.match(LONG_RUN_HINT, /二十多分钟/)
    assert.equal(LONG_RUN_HINT.includes('剩余'), false)
    assert.equal(LONG_RUN_HINT.toLowerCase().includes('ltx'), false)
  })
})
