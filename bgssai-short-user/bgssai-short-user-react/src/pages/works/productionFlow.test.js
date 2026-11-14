import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildProductionFlow,
  scriptStudioGuide,
  resolveRepairAction,
  selectPendingVideoShotIds,
} from './productionFlow.js'

test('guides creator step-by-step from empty work to finalized script, look lock, storyboard video, and compose', () => {
  const stepOutline = buildProductionFlow({
    episode: { script_status: 'NONE' },
    outlineCount: 0,
    scriptSceneCount: 0,
  })
  assert.equal(stepOutline.nextAction.primaryTab, 'script')
  assert.match(stepOutline.nextAction.title, /大纲/)

  const stepGenerateScript = buildProductionFlow({
    episode: { script_status: 'NONE' },
    outlineCount: 3,
    scriptSceneCount: 0,
  })
  assert.equal(stepGenerateScript.nextAction.primaryTab, 'script')
  assert.equal(stepGenerateScript.nextAction.primaryLabel, '去生成本集剧本')

  const stepReadyScript = buildProductionFlow({
    episode: { script_status: 'DRAFT' },
    outlineCount: 3,
    scriptSceneCount: 4,
  })
  assert.equal(stepReadyScript.nextAction.primaryTab, 'script')
  assert.equal(stepReadyScript.nextAction.primaryLabel, '去完成剧本定稿')

  const stepLockCandidates = buildProductionFlow({
    episode: { script_status: 'READY' },
    outlineCount: 3,
    scriptSceneCount: 4,
    consistency: {
      character_list: [{
        character_id: 'c-1',
        character_name: '沈知微',
        card_status: 'DRAFT',
        candidate_list: [{ asset_id: 'a-1', file_url: 'https://cdn.example/c1.jpg' }],
      }],
      scene_list: [],
      prop_list: [],
    },
    shots: [],
  })
  assert.equal(stepLockCandidates.nextAction.primaryTab, 'characters')
  assert.match(stepLockCandidates.nextAction.title, /选定/)

  const stepSplit = buildProductionFlow({
    episode: { script_status: 'READY' },
    outlineCount: 3,
    scriptSceneCount: 4,
    consistency: {
      character_list: [{
        character_id: 'c-1',
        character_name: '沈知微',
        card_status: 'LOCKED',
        ref_image_asset_id: 'a-1',
      }],
      scene_list: [],
      prop_list: [],
    },
    shots: [],
  })
  assert.equal(stepSplit.nextAction.primaryTab, 'storyboard')
  assert.equal(stepSplit.nextAction.primaryLabel, '去拆解分镜')

  const stepBlockedShot = buildProductionFlow({
    episode: { script_status: 'READY' },
    consistency: {
      character_list: [{ character_id: 'c-1', character_name: '沈知微', card_status: 'DRAFT' }],
      unlocked_ref_count: 1,
    },
    shots: [{
      shot_id: 's-1',
      shot_status: 'PENDING',
      blocked_flag: 1,
      block_reason: '待锁定定妆：角色「沈知微」',
    }],
  })
  assert.equal(stepBlockedShot.nextAction.primaryTab, 'characters')
  assert.match(stepBlockedShot.nextAction.description, /沈知微/)

  const shotsPendingVideo = [
    { shot_id: 's-1', shot_status: 'VIDEO_READY', blocked_flag: 0, video_url: '/assets/s1.mp4' },
    { shot_id: 's-2', shot_status: 'IMAGE_READY', blocked_flag: 0, first_frame_url: '/assets/s2.jpg' },
    { shot_id: 's-3', shot_status: 'VIDEO_GENERATING', blocked_flag: 0, latest_task: { task_type: 'VIDEO', task_status: 'RUNNING' } },
  ]
  assert.deepEqual(selectPendingVideoShotIds(shotsPendingVideo), ['s-2'])

  const stepGenerateVideos = buildProductionFlow({
    episode: { script_status: 'READY' },
    consistency: { character_list: [], scene_list: [], prop_list: [] },
    shots: [
      { shot_id: 's-1', shot_status: 'VIDEO_READY', blocked_flag: 0, video_url: '/assets/s1.mp4' },
      { shot_id: 's-2', shot_status: 'IMAGE_READY', blocked_flag: 0, first_frame_url: '/assets/s2.jpg' },
    ],
  })
  assert.equal(stepGenerateVideos.nextAction.primaryTab, 'storyboard')
  assert.equal(stepGenerateVideos.metrics.videoReadyCount, 1)

  const stepReadyToCompose = buildProductionFlow({
    episode: { script_status: 'READY', compose_status: 'NONE' },
    shots: [
      { shot_id: 's-1', shot_status: 'VIDEO_READY', blocked_flag: 0, video_url: '/assets/s1.mp4' },
      { shot_id: 's-2', shot_status: 'LOCKED', blocked_flag: 0, video_url: '/assets/s2.mp4' },
    ],
  })
  assert.equal(stepReadyToCompose.nextAction.primaryTab, 'compose')
  assert.equal(stepReadyToCompose.nextAction.secondaryTab, 'voice')

  const stepCompleted = buildProductionFlow({
    episode: { script_status: 'READY', compose_status: 'READY' },
    shots: [
      { shot_id: 's-1', shot_status: 'VIDEO_READY', blocked_flag: 0, video_url: '/assets/s1.mp4' },
    ],
    composeEntry: { phase: 'settled', film: { compose_status: 'READY', expired_flag: 0 } },
  })
  assert.equal(stepCompleted.steps[4].status, 'done')
  assert.equal(stepCompleted.nextAction.primaryLabel, '查看与下载成片')
})

test('a finalized episode that already has a film does not keep step 1 as the next action', () => {
  const filmed = scriptStudioGuide({ scriptStatus: 'READY', composeStatus: 'READY' })
  assert.equal(filmed.mode, 'filmed')
  assert.equal(filmed.offerGenerate, false)
  assert.equal(filmed.title.includes('第 1 步'), false)
  assert.match(filmed.detail, /成片/)
  const editing = scriptStudioGuide({ scriptStatus: 'READY', composeStatus: 'NONE' })
  assert.equal(editing.mode, 'editing')
  assert.equal(editing.offerGenerate, true)
  assert.match(editing.title, /第 1 步/)
})

test('resolveRepairAction routes quota, look, and script errors to exact destination', () => {
  assert.equal(resolveRepairAction('镜1 引用的角色「沈知微」尚未定妆').tab, 'characters')
  assert.equal(resolveRepairAction('可用余额不足，请先充值').href, '/subscription')
  assert.equal(resolveRepairAction('请先完成本集定稿').tab, 'script')
  assert.equal(resolveRepairAction('服务繁忙，请稍后查看任务').href, '/tasks')
})
