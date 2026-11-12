import test from 'node:test'
import assert from 'node:assert/strict'
import {
  ATTENTION_LIMIT,
  SAMPLE_FLOW_LABEL,
  SAMPLE_FLOW_PATH,
  attentionItems,
  ownedSamplePath,
  continuePath,
  coverUrl,
  episodeProgress,
  nextStepText,
} from './homeWorkbench.js'

const work = (id, title, extra = {}) => ({ work_id: id, work_title: title, episode_count: 24, ...extra })

test('sample flow stays a small link target, not a promo route', () => {
  assert.equal(SAMPLE_FLOW_LABEL, '看示例流程')
  assert.equal(SAMPLE_FLOW_PATH, '/works/w-dm-005')
  assert.equal(ownedSamplePath([{ work_id: 'w-dm-005', work_title: '悬疑法医手记' }]), '/works/w-dm-005')
  assert.equal(ownedSamplePath([{ work_id: 'w9', work_title: '别的剧' }]), null)
})

test('cover is omitted unless the API already returned an image URL', () => {
  assert.equal(coverUrl({ work_title: '无封面' }), null)
  assert.equal(coverUrl({ cover_asset_id: 'as-dm-005-cover' }), null)
  assert.equal(coverUrl({ cover_url: '  ' }), null)
  assert.equal(coverUrl({ cover_url: 'stub://cover' }), null)
  assert.equal(coverUrl({ cover_url: ' https://cdn.example/cover.png ' }), 'https://cdn.example/cover.png')
})

test('episode progress falls back to the count when detail has no episodes', () => {
  assert.deepEqual(episodeProgress(work('w1', '甲'), null), { total: 24, percent: null, label: '24 集' })
  assert.deepEqual(episodeProgress(work('w1', '甲'), []), { total: 24, percent: null, label: '24 集' })
})

test('episode progress prefers finished films, otherwise scripts', () => {
  const episodes = [
    { script_status: 'READY', compose_status: 'READY', review_status: 'APPROVED' },
    { script_status: 'READY', compose_status: 'NONE', review_status: 'NOT_SUBMITTED' },
    { script_status: 'NONE', compose_status: 'NONE', review_status: 'NOT_SUBMITTED' },
  ]
  assert.deepEqual(episodeProgress({ episode_count: 3 }, episodes), {
    total: 3,
    percent: 33,
    label: '成片 1/3',
  })
  assert.equal(episodeProgress({ episode_count: 2 }, [
    { script_status: 'DRAFT', compose_status: 'NONE' },
    { script_status: 'NONE', compose_status: 'NONE' },
  ]).label, '剧本 1/2')
})

test('continue opens the step that is actually next', () => {
  assert.equal(continuePath(work('w1', '甲'), []), '/works/w1/script')
  assert.equal(continuePath(work('w1', '甲'), [
    { episode_id: 'e2', review_status: 'PENDING', script_status: 'READY' },
  ]), '/works/w1/compose?episode_id=e2')
  assert.equal(continuePath(work('w1', '甲'), [
    { episode_id: 'e1', review_status: 'APPROVED', script_status: 'READY' },
    { episode_id: 'e3', review_status: 'NOT_SUBMITTED', script_status: 'READY', storyboard_status: 'DRAFT' },
  ]), '/works/w1/storyboard?episode_id=e3')
  assert.equal(continuePath(work('w1', '甲'), [
    { episode_id: 'e4', review_status: 'NOT_SUBMITTED', script_status: 'READY', storyboard_status: 'FAILED' },
  ]), '/works/w1/storyboard?episode_id=e4')
})

test('attention is empty when nothing is stuck', () => {
  const items = attentionItems({
    works: [work('w1', '甲')],
    episodesById: { w1: [{ episode_id: 'e1', episode_no: 1, review_status: 'NOT_SUBMITTED', script_status: 'DRAFT' }] },
    failedByWorkId: { w1: [] },
  })
  assert.deepEqual(items, [])
})

test('failed generation and pending review deep-link, and an empty list stays empty', () => {
  const items = attentionItems({
    works: [work('w1', '悬疑法医手记'), work('w2', '退婚')],
    episodesById: {
      w1: [{ episode_id: 'e1', episode_no: 1, review_status: 'PENDING', script_status: 'READY' }],
      w2: [{ episode_id: 'e9', episode_no: 9, review_status: 'REJECTED', compose_status: 'FAILED' }],
    },
    failedByWorkId: {
      w1: [{ task_status: 'FAILED', fail_reason_code: 'MODEL_TIMEOUT', fail_reason: '生成超时', task_type_name: '视频' }],
      w2: [],
    },
  })
  assert.equal(items[0].title, '悬疑法医手记 · 第1集待审核')
  assert.equal(items[0].to, '/works/w1/compose?episode_id=e1')
  assert.equal(items[1].kind, 'generate_failed')
  assert.equal(items[1].to, '/works/w1/tasks')
  assert.match(items[1].detail, /超时/)
  assert.equal(items[2].title, '退婚 · 第9集审核驳回')
  assert.equal(items[3].to, '/works/w2/compose?episode_id=e9')
})

test('a failed task replaces the episode-stage failure card for the same work', () => {
  const items = attentionItems({
    works: [work('w1', '甲')],
    episodesById: {
      w1: [{ episode_id: 'e1', episode_no: 1, review_status: 'NOT_SUBMITTED', storyboard_status: 'FAILED' }],
    },
    failedByWorkId: { w1: [{ task_status: 'FAILED', fail_reason_code: 'ASSET_MISSING', fail_reason: '缺首帧', task_type: 'IMAGE' }] },
  })
  assert.equal(items.length, 1)
  assert.equal(items[0].to, '/works/w1/tasks')
})

test('attention list is capped', () => {
  const works = []
  const episodesById = {}
  for (let i = 0; i < 8; i += 1) {
    const id = `w${i}`
    works.push(work(id, `剧${i}`))
    episodesById[id] = [{ episode_id: `e${i}`, episode_no: 1, review_status: 'PENDING' }]
  }
  const items = attentionItems({ works, episodesById, failedByWorkId: {} })
  assert.equal(items.length, ATTENTION_LIMIT)
})

test('next step answers what to do, including the empty and failed cases', () => {
  assert.equal(nextStepText({ loadError: true, works: [], attention: [] }), '作品列表暂时读不到，刷新后再看下一步。')
  assert.match(nextStepText({ works: [], attention: [] }), /新建一部作品/)
  assert.equal(nextStepText({
    works: [work('w1', '悬疑法医手记')],
    attention: [],
  }), '接着做《悬疑法医手记》。')
  assert.match(nextStepText({
    works: [work('w1', '悬疑法医手记')],
    attention: [{ kind: 'generate_failed', workTitle: '悬疑法医手记' }],
  }), /生成失败/)
  assert.match(nextStepText({
    works: [work('w1', '甲')],
    attention: [{ kind: 'review_pending', workTitle: '甲' }],
  }), /等审核/)
})
