import test from 'node:test'
import assert from 'node:assert/strict'
import {
  buildVideoPathInput,
  resolveVideoPath,
  shotsForFrames,
  shotsForVideo,
  unlockedLooks,
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

test('a draft script asks to finalize, and a low score offers force', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'DRAFT',
    sceneCount: 2,
    totalScore: 42,
    episodeNo: 1,
  })
  assert.equal(step.key, 'ready')
  assert.equal(step.action, '定稿这一集')
  assert.equal(step.secondary.kind, 'ready-force')
  assert.match(step.detail, /42/)
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
  assert.deepEqual(looks.map((item) => item.name), ['苏曼', '客厅'])
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    sceneCount: 2,
    looks,
    shotCount: 0,
    episodeNo: 1,
  })
  assert.equal(step.key, 'looks')
  assert.equal(step.kind, 'lock')
  assert.equal(step.look.id, 'c1')
  assert.equal(step.tab, 'characters')
})

test('a card without a picture asks for a portrait before the storyboard', () => {
  const step = path({
    outlineCount: 1,
    scriptStatus: 'READY',
    looks: [{ kind: 'scene', id: 's1', name: '客厅', candidateId: '' }],
    shotCount: 3,
    shots: [],
    episodeNo: 1,
  })
  assert.equal(step.kind, 'portrait')
  assert.match(step.detail, /2 张/)
  assert.match(step.detail, /失败/)
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
  assert.deepEqual(step.doneKeys, ['outline', 'script', 'ready', 'looks'])
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
