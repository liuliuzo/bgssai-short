import test from 'node:test'
import assert from 'node:assert/strict'
import {
  classifyVideoDuration,
  shotCardHeading,
  shotHasBoundVideo,
  shotHasPlayableVideo,
  shotCardStatusCode,
  shotLatestTaskBadge,
  shotCanvasStatusKey,
  shotOffersVideoRegenerate,
  shotVideoRegenerateBlocked,
} from './shotCardMedia.js'

test('a ready clip with a failed video job stays a video without a standing failure chip', () => {
  const row = {
    shot_status: 'VIDEO_READY',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    first_frame_url: 'https://cdn.example/frame.png',
    latest_task: {
      task_type: 'VIDEO',
      task_type_name: '视频',
      task_status: 'FAILED',
      task_status_name: '已失败',
    },
  }
  assert.equal(shotHasPlayableVideo(row), true)
  assert.equal(shotCardStatusCode(row), 'VIDEO_READY')
  assert.equal(shotLatestTaskBadge(row), null)
  assert.equal(shotCanvasStatusKey(row), 'VIDEO_READY')
})

test('a locked clip does not keep a stale regen failure chip', () => {
  const row = {
    shot_status: 'LOCKED',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    latest_task: {
      task_type: 'VIDEO',
      task_type_name: '视频',
      task_status: 'FAILED',
      task_status_name: '已失败',
    },
  }
  assert.equal(shotCardStatusCode(row), 'LOCKED')
  assert.equal(shotLatestTaskBadge(row), null)
  assert.equal(shotCanvasStatusKey(row), 'LOCKED')
})

test('an in-flight failed regen can still say the existing clip remains', () => {
  const row = {
    shot_status: 'VIDEO_GENERATING',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    latest_task: {
      task_type: 'VIDEO',
      task_type_name: '视频',
      task_status: 'FAILED',
      task_status_name: '已失败',
    },
  }
  const badge = shotLatestTaskBadge(row)
  assert.equal(badge.color, 'gold')
  assert.equal(badge.text, '已有视频，本次重生成失败')
  assert.equal(badge.text.includes('已失败'), false)
})

test('official video queue is not shown as running', () => {
  const row = {
    shot_status: 'VIDEO_GENERATING',
    video_placeholder_flag: 1,
    video_asset_id: 'as-dm-001-s01v01',
    video_url: 'data:image/svg+xml;base64,abc',
    latest_task: {
      task_type: 'VIDEO',
      task_type_name: '视频',
      task_status: 'QUEUED',
      task_status_name: '排队中',
      status_note: '前面还有 2 条',
    },
  }
  assert.equal(shotHasBoundVideo(row), false)
  assert.match(shotLatestTaskBadge(row).text, /排队中/)
  assert.match(shotLatestTaskBadge(row).text, /前面还有 2 条/)
  assert.equal(shotLatestTaskBadge(row).text.includes('执行中'), false)
})

test('a settled ready clip still shows a video job that is actually running', () => {
  const row = {
    shot_status: 'VIDEO_READY',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    latest_task: {
      task_type: 'VIDEO',
      task_type_name: '视频',
      task_status: 'RUNNING',
      task_status_name: '执行中',
    },
  }
  assert.equal(shotLatestTaskBadge(row).color, 'blue')
  assert.match(shotLatestTaskBadge(row).text, /执行中/)
})

test('a settled ready clip does not keep a canceled regen chip', () => {
  const row = {
    shot_status: 'VIDEO_READY',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    latest_task: {
      task_type: 'VIDEO',
      task_status: 'CANCELED',
      task_status_name: '已取消',
      task_type_name: '视频',
    },
  }
  assert.equal(shotLatestTaskBadge(row), null)
})

test('generating status does not hide a bound clip', () => {
  const row = {
    shot_status: 'VIDEO_GENERATING',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    latest_task: { task_type: 'VIDEO', task_type_name: '视频', task_status: 'RUNNING', task_status_name: '执行中' },
  }
  assert.equal(shotCardStatusCode(row), 'VIDEO_READY')
  assert.equal(shotLatestTaskBadge(row).color, 'blue')
  assert.match(shotLatestTaskBadge(row).text, /执行中/)
})

test('canceled regen still points at the existing clip', () => {
  const row = {
    shot_status: 'VIDEO_GENERATING',
    video_asset_id: 'vid-1',
    video_url: '',
    latest_task: { task_type: 'VIDEO', task_status: 'CANCELED', task_status_name: '已取消', task_type_name: '视频' },
  }
  assert.equal(shotHasBoundVideo(row), true)
  assert.equal(shotHasPlayableVideo(row), false)
  assert.equal(shotLatestTaskBadge(row).text, '已有视频，本次生成已取消')
})

test('a failed video with no clip stays a failure', () => {
  const row = {
    shot_status: 'VIDEO_GENERATING',
    video_asset_id: '  ',
    video_url: null,
    latest_task: { task_type: 'VIDEO', task_type_name: '视频', task_status: 'FAILED', task_status_name: '已失败' },
  }
  assert.equal(shotHasBoundVideo(row), false)
  assert.equal(shotCardStatusCode(row), 'VIDEO_GENERATING')
  const badge = shotLatestTaskBadge(row)
  assert.equal(badge.color, 'red')
  assert.equal(badge.text, '视频 已失败')
})

test('locked shots keep the locked title and still count as video on the canvas', () => {
  const row = { shot_status: 'LOCKED', video_url: 'https://cdn.example/shot.mp4', blocked_flag: 1 }
  assert.equal(shotCardStatusCode(row), 'LOCKED')
  assert.equal(shotCanvasStatusKey(row), 'LOCKED')
})

test('image failure without a video does not borrow the video chip', () => {
  const row = {
    shot_status: 'IMAGE_READY',
    first_frame_url: 'https://cdn.example/frame.png',
    latest_task: { task_type: 'IMAGE', task_type_name: '图片', task_status: 'FAILED', task_status_name: '已失败' },
  }
  assert.equal(shotHasPlayableVideo(row), false)
  assert.equal(shotCardStatusCode(row), 'IMAGE_READY')
  assert.equal(shotLatestTaskBadge(row).color, 'red')
  assert.equal(shotLatestTaskBadge(row).text, '图片 已失败')
})

test('a seed svg is not a playable video and is not labeled as a finished clip', () => {
  const row = {
    shot_no: 1,
    shot_status: 'VIDEO_READY',
    shot_duration_sec: 3,
    video_url: "data:image/svg+xml;base64,PHN2ZyBmaWxsPScjN2MzYWVkJz48L3N2Zz4=",
    video_asset_id: 'as-seed',
    latest_task: { task_type: 'VIDEO', task_type_name: '视频', task_status: 'SUCCEEDED', task_status_name: '已成功' },
  }
  assert.equal(shotHasPlayableVideo(row), false)
  assert.equal(shotHasBoundVideo(row), false)
  assert.equal(shotLatestTaskBadge(row).text, '这是占位图，不能播放')
  assert.match(shotCardHeading(row), /占位图，不能播放/)
  assert.doesNotMatch(shotCardHeading(row), /成片/)
})

test('a finished official clip offers regenerate, and a running one does not submit again', () => {
  const ready = {
    shot_status: 'VIDEO_READY',
    video_asset_id: 'vid-1',
    video_url: 'https://cdn.example/shot.mp4',
    latest_task: { task_type: 'VIDEO', task_status: 'SUCCEEDED' },
  }
  assert.equal(shotOffersVideoRegenerate(ready), true)
  assert.equal(shotVideoRegenerateBlocked(ready), false)
  const running = {
    ...ready,
    shot_status: 'VIDEO_GENERATING',
    latest_task: { task_type: 'VIDEO', task_status: 'QUEUED' },
  }
  assert.equal(shotOffersVideoRegenerate(running), true)
  assert.equal(shotVideoRegenerateBlocked(running), true)
  const seed = {
    shot_status: 'VIDEO_READY',
    video_placeholder_flag: 1,
    video_url: 'data:image/svg+xml;base64,abc',
  }
  assert.equal(shotOffersVideoRegenerate(seed), true)
  const failed = {
    shot_status: 'IMAGE_READY',
    latest_task: { task_type: 'VIDEO', task_status: 'FAILED' },
  }
  assert.equal(shotOffersVideoRegenerate(failed), true)
  assert.equal(shotOffersVideoRegenerate({ shot_status: 'LOCKED', video_url: 'https://cdn.example/shot.mp4' }), false)
  assert.equal(shotOffersVideoRegenerate({ shot_status: 'IMAGE_READY' }, 'broken'), true)
})

test('a frame with no duration is a broken clip, not a success', () => {
  assert.equal(classifyVideoDuration(0, { hasFrame: true }), 'broken')
  assert.equal(classifyVideoDuration(Number.NaN, { hasFrame: true }), 'broken')
  assert.equal(classifyVideoDuration(6, { hasFrame: true }), 'ready')
  assert.equal(classifyVideoDuration(Number.NaN, { hasFrame: false }), 'pending')
  const row = { shot_no: 2, shot_status: 'VIDEO_READY', shot_duration_sec: 3, video_url: 'https://cdn.example/shot.mp4' }
  assert.match(shotCardHeading(row, { playback: 'broken' }), /这段视频打不开/)
  assert.match(shotCardHeading(row, { actualSec: 6 }), /成片 6秒 · 计划 3秒/)
})
