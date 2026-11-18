import test from 'node:test'
import assert from 'node:assert/strict'
import {
  AUX_STUDIO_TOOLS,
  CORE_STUDIO_TABS,
  VIDEO_PATH_STEPS,
  buildStepItems,
  buildVideoPathInput,
  resolveScriptRhythmScore,
  resolveVideoPath,
  resolveViewingStageKey,
  scriptReadyDecision,
  shotsForFrames,
  shotsForVideo,
  unlockedLooks,
  unlockedScenes,
} from './productionGuide.js'

const episode = (extra = {}) => ({ episode_id: 'e1', episode_no: 1, script_status: 'NONE', compose_status: 'NONE', ...extra })

function path(input) {
  return resolveVideoPath(input)
}

test('empty work starts at the outline', () => {
  const step = path({ outlineCount: 0, episodeNo: 1 })
  assert.equal(step.key, 'outline')
  assert.equal(step.action, '生成大纲')
  assert.equal(step.tab, 'script')
  assert.match(step.title, /第1集/)
})

test('a running outline waits instead of submitting again', () => {
  const step = path({ outlineCount: 0, outlineRunning: true, episodeNo: 2 })
  assert.equal(step.waiting, true)
  assert.equal(step.action, '')
  assert.match(step.title, /大纲正在生成/)
})

test('a failed outline asks to generate again', () => {
  const step = path({ outlineCount: 0, outlineFailed: true })
  assert.equal(step.action, '重新生成大纲')
})

test('outline without a script goes to script generation', () => {
  const step = path({ outlineCount: 3, scriptStatus: 'NONE', sceneCount: 0, episodeNo: 1 })
  assert.equal(step.key, 'script')
  assert.equal(step.kind, 'script')
  assert.equal(step.action, '生成这一集剧本')
})

test('a draft script exposes a single finalize action without a force button', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'DRAFT',
    sceneCount: 2,
    totalScore: 42,
    episodeNo: 1,
  })
  assert.equal(step.key, 'ready')
  assert.equal(step.action, '定稿')
  assert.equal(step.secondary, null)
  assert.match(step.detail, /42/)
  assert.doesNotMatch(step.detail, /强制定稿|定稿这一集|定稿剧本/)
})

test('scriptReadyDecision gates below 60 with confirmation and passes 60+ directly', () => {
  assert.equal(resolveScriptRhythmScore({ rhythm_check: { total_score: 75 }, total_score: 50 }), 75)
  assert.equal(resolveScriptRhythmScore({ total_score: 0 }), 0)
  assert.equal(resolveScriptRhythmScore(null), null)
  assert.equal(resolveScriptRhythmScore(''), null)

  assert.deepEqual(scriptReadyDecision({ total_score: 60 }), {
    needsConfirm: false,
    forceReadyFlag: 0,
    score: 60,
  })
  assert.deepEqual(scriptReadyDecision({ rhythm_check: { total_score: 70 } }), {
    needsConfirm: false,
    forceReadyFlag: 0,
    score: 70,
  })

  const low = scriptReadyDecision({ total_score: 42 })
  assert.equal(low.needsConfirm, true)
  assert.equal(low.forceReadyFlag, 1)
  assert.equal(low.score, 42)
  assert.equal(low.okText, '定稿')
  assert.equal(low.cancelText, '取消')
  assert.match(low.confirmContent, /42 分/)

  for (const blankOrZero of [0, null, undefined, {}]) {
    const decision = scriptReadyDecision(blankOrZero)
    assert.equal(decision.needsConfirm, true)
    assert.equal(decision.forceReadyFlag, 1)
    assert.equal(decision.okText, '定稿')
    assert.equal(decision.cancelText, '取消')
    assert.match(decision.confirmContent, /未满 60 分/)
  }
})

test('unlocked cards block frames, and an existing candidate is locked for free', () => {
  const panel = {
    character_list: [
      { character_id: 'c1', character_name: '苏曼', card_status: 'DRAFT', candidate_list: [{ asset_id: 'a1' }] },
      { character_id: 'c2', character_name: '周衡', card_status: 'LOCKED', ref_image_asset_id: 'a2' },
    ],
    scene_list: [{ scene_card_id: 's1', card_name: '客厅', card_status: 'DRAFT', candidate_list: [] }],
    prop_list: [],
  }
  const looks = unlockedLooks(panel)
  assert.deepEqual(looks.map((item) => item.name), ['苏曼'])
  assert.deepEqual(unlockedScenes(panel).map((item) => item.name), ['客厅'])
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    sceneCount: 2,
    looks,
    sceneRefs: unlockedScenes(panel),
    shotCount: 0,
    episodeNo: 1,
  })
  assert.equal(step.key, 'looks')
  assert.equal(step.kind, 'lock')
  assert.equal(step.look.id, 'c1')
  assert.equal(step.tab, 'characters')
})

test('a scene card without a picture asks for a reference image in its own step', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    sceneRefs: [{ kind: 'scene', id: 's1', name: '客厅', candidateId: '' }],
    shotCount: 3,
    shots: [],
    episodeNo: 1,
  })
  assert.equal(step.key, 'scenes')
  assert.equal(step.tab, 'scenes')
  assert.equal(step.stepNo, 5)
  assert.equal(step.kind, 'portrait')
  assert.match(step.detail, /2 张/)
  assert.match(step.detail, /失败/)
})

test('locked looks move to scenes, and scene portrait wait only blocks the scenes step', () => {
  const panel = {
    character_list: [{ character_id: 'c1', character_name: '苏曼', card_status: 'LOCKED', ref_image_asset_id: 'a1' }],
    scene_list: [{ scene_card_id: 's1', card_name: '客厅', card_status: 'DRAFT', candidate_list: [{ asset_id: 'a9' }] }],
    prop_list: [],
  }
  const lockInput = buildVideoPathInput({
    outline: { outline: [{ episode_no: 1 }] },
    script: { script_status: 'READY', scene_list: [{ scene_no: 1 }] },
    panel,
    shots: [],
    tasks: [{ target_type: 'PORTRAIT', task_status: 'RUNNING' }],
    episode: episode({ script_status: 'READY' }),
  })
  const lockStep = path(lockInput)
  assert.equal(lockStep.key, 'scenes')
  assert.equal(lockStep.kind, 'lock')
  assert.equal(lockStep.look.id, 's1')

  const waitInput = buildVideoPathInput({
    outline: { outline: [{ episode_no: 1 }] },
    script: { script_status: 'READY', scene_list: [{ scene_no: 1 }] },
    panel,
    shots: [],
    tasks: [{ target_type: 'SCENE_PORTRAIT', task_status: 'RUNNING' }],
    episode: episode({ script_status: 'READY' }),
  })
  const waitStep = path(waitInput)
  assert.equal(waitStep.key, 'scenes')
  assert.equal(waitStep.waiting, true)
})

test('ready script with no open cards splits into shots', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'LOCKED',
    looks: [],
    shotCount: 0,
    shots: [],
    episodeNo: 1,
  })
  assert.equal(step.key, 'shots')
  assert.equal(step.kind, 'split')
  assert.equal(step.action, '从剧本拆镜')
  assert.deepEqual(step.doneKeys, ['outline', 'script', 'ready', 'looks', 'scenes'])
})

test('shots without a picture description stay on spoken previz', () => {
  const shots = [{ shot_id: 'sh1', shot_status: 'PENDING', shot_desc: '短' }]
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 1,
    shots,
    videoCount: 0,
    episodeNo: 1,
  })
  assert.equal(step.kind, 'previz')
})

test('framed shots are the only ones sent to video, and bare shots go to stills first', () => {
  const shots = [
    { shot_id: 'a', shot_status: 'IMAGE_READY', first_frame_asset_id: 'f1', shot_desc: '客厅里苏曼回头' },
    { shot_id: 'b', shot_status: 'PENDING', shot_desc: '客厅里周衡推门' },
  ]
  assert.deepEqual(shotsForFrames(shots).map((row) => row.shot_id), ['b'])
  assert.deepEqual(shotsForVideo(shots).map((row) => row.shot_id), ['a'])
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 2,
    shots,
    videoCount: 0,
    episodeNo: 1,
  })
  assert.equal(step.key, 'frame')
  assert.equal(step.action, '出首帧')
})

test('a running still job does not offer another submit', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 1,
    shots: [{ shot_id: 'a', shot_status: 'IMAGE_GENERATING', shot_desc: '客厅里苏曼回头' }],
    videoCount: 0,
    imageShotRunning: true,
    episodeNo: 1,
  })
  assert.equal(step.waiting, true)
  assert.match(step.title, /首帧正在生成/)
})

test('stills that exist move to video, then to the film page', () => {
  const shots = [{ shot_id: 'a', shot_status: 'IMAGE_READY', first_frame_url: 'https://cdn.example/a.png', shot_desc: '客厅里苏曼回头' }]
  const video = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 1,
    shots,
    videoCount: 0,
    episodeNo: 1,
  })
  assert.equal(video.key, 'video')
  assert.equal(video.action, '生成视频')

  const filmed = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 1,
    shots: [{ shot_id: 'a', shot_status: 'VIDEO_READY', video_asset_id: 'v1', first_frame_asset_id: 'f1' }],
    videoCount: 1,
    composeStatus: 'NONE',
    episodeNo: 1,
  })
  assert.equal(filmed.key, 'film')
  assert.equal(filmed.action, '去合成成片')
  assert.equal(filmed.tab, 'compose')
})

test('a ready film is the end of the path', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 1,
    shots: [{ shot_id: 'a', shot_status: 'LOCKED', video_url: 'https://cdn.example/a.mp4' }],
    videoCount: 1,
    composeStatus: 'READY',
    episodeNo: 1,
  })
  assert.equal(step.action, '查看成片')
  assert.ok(step.doneKeys.includes('video'))
  assert.ok(step.doneKeys.includes('film'))
})

test('api payload maps into the same path', () => {
  const input = buildVideoPathInput({
    outline: { outline: [{ episode_no: 1 }] },
    script: { script_status: 'READY', scene_list: [{ scene_no: 1 }], total_score: 80 },
    panel: { character_list: [], scene_list: [], prop_list: [] },
    shots: [],
    tasks: [{ target_type: 'OUTLINE', task_status: 'SUCCEEDED' }],
    episode: episode({ script_status: 'READY' }),
  })
  assert.equal(input.outlineCount, 1)
  assert.equal(input.sceneCount, 1)
  assert.equal(resolveVideoPath(input).kind, 'split')
})

test('every step carries stepNo and tab, and allDone marks step 9 as 已完成', () => {
  assert.equal(VIDEO_PATH_STEPS.length, 9)
  assert.deepEqual(
    VIDEO_PATH_STEPS.map((item) => [item.stepNo, item.key, item.tab]),
    [
      [1, 'outline', 'script'],
      [2, 'script', 'script'],
      [3, 'ready', 'script'],
      [4, 'looks', 'characters'],
      [5, 'scenes', 'scenes'],
      [6, 'shots', 'storyboard'],
      [7, 'frame', 'storyboard'],
      [8, 'video', 'storyboard'],
      [9, 'film', 'compose'],
    ],
  )
  assert.ok(CORE_STUDIO_TABS.has('script'))
  assert.ok(CORE_STUDIO_TABS.has('scenes'))
  assert.equal(AUX_STUDIO_TOOLS.length, 8)

  const finished = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [],
    shotCount: 1,
    shots: [{ shot_id: 'a', shot_status: 'LOCKED', video_url: 'https://cdn.example/a.mp4' }],
    videoCount: 1,
    composeStatus: 'READY',
  })
  assert.equal(finished.stepNo, 9)
  assert.equal(finished.stepLabel, '成片')
  assert.equal(finished.allDone, true)
  const items = buildStepItems(finished, 'film')
  assert.ok(items.every((item) => item.state === 'done' && item.statusLabel === '已完成'))
  assert.equal(items[8].isViewing, true)
})

test('buildStepItems separates current stage from historical viewing stage and running stage', () => {
  const atLooks = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [{ kind: 'character', id: 'c1', name: '李伟', candidateId: '' }],
  })
  assert.equal(atLooks.stepNo, 4)
  assert.equal(atLooks.stepLabel, '定妆')
  assert.equal(atLooks.allDone, false)

  const items = buildStepItems(atLooks, 'outline')
  assert.equal(items[0].state, 'done')
  assert.equal(items[0].statusLabel, '已完成')
  assert.equal(items[0].isViewing, true)
  assert.equal(items[3].state, 'current')
  assert.equal(items[3].statusLabel, '当前环节')
  assert.equal(items[3].isViewing, false)
  assert.equal(items[4].state, 'todo')
  assert.equal(items[4].statusLabel, '未开始')

  const running = path({ outlineCount: 0, outlineRunning: true })
  const runningItems = buildStepItems(running, 'outline')
  assert.equal(runningItems[0].state, 'running')
  assert.equal(runningItems[0].statusLabel, '生成中')
})

test('resolveViewingStageKey maps core tabs, stage query, and aux tools', () => {
  const currentAtLooks = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [{ kind: 'character', id: 'c1', name: '李伟', candidateId: '' }],
  })
  assert.equal(
    resolveViewingStageKey({ activeTab: 'script', requestedStage: 'outline', currentStep: currentAtLooks, outlineCount: 3 }),
    'outline',
  )
  assert.equal(
    resolveViewingStageKey({ activeTab: 'script', requestedStage: 'looks', currentStep: currentAtLooks, outlineCount: 3 }),
    'script',
  )
  assert.equal(
    resolveViewingStageKey({ activeTab: 'script', requestedStage: '', currentStep: currentAtLooks, outlineCount: 0 }),
    'outline',
  )
  assert.equal(
    resolveViewingStageKey({ activeTab: 'characters', requestedStage: '', currentStep: currentAtLooks, outlineCount: 3 }),
    'looks',
  )
  assert.equal(
    resolveViewingStageKey({ activeTab: 'scenes', requestedStage: '', currentStep: currentAtLooks, outlineCount: 3 }),
    'scenes',
  )
  assert.equal(
    resolveViewingStageKey({ activeTab: 'storyboard', requestedStage: 'frame', currentStep: currentAtLooks, outlineCount: 3 }),
    'frame',
  )
  assert.equal(
    resolveViewingStageKey({ activeTab: 'source', requestedStage: 'outline', currentStep: currentAtLooks, outlineCount: 3 }),
    null,
  )
})
