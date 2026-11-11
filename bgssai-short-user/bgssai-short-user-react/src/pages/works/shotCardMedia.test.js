import test from 'node:test'
import assert from 'node:assert/strict'
import {
  shotHasBoundVideo,
  shotHasPlayableVideo,
  shotCardStatusCode,
  shotLatestTaskBadge,
  shotCanvasStatusKey,
} from './shotCardMedia.js'

test('a ready clip with a failed video job stays a video', () => {
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
  const badge = shotLatestTaskBadge(row)
  assert.equal(badge.color, 'gold')
  assert.equal(badge.text, '已有视频，本次重生成失败')
  assert.equal(badge.text.includes('已失败'), false)
  assert.equal(shotCanvasStatusKey(row), 'VIDEO_READY')
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
