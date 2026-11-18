import test from 'node:test'
import assert from 'node:assert/strict'
import {
  ATTENTION_LIMIT,
  SAMPLE_FLOW_LABEL,
  SAMPLE_FLOW_PATH,
  attentionItems,
  ownedSamplePath,
  continuePath,
  continueLabel,
  coverUrl,
  CARD_EPISODE_INLINE_LIMIT,
  episodeCardSummary,
  episodeProgress,
  episodeStageItems,
  findEpisodeItem,
  nextStepText,
  resolveEpisodeStage,
  workCardStatus,
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
  assert.deepEqual(episodeProgress(work('w1', '甲'), null), {
    total: 24,
    filmed: 0,
    percent: null,
    label: '共 24 集',
    episodeItems: [],
  })
  assert.deepEqual(episodeProgress(work('w1', '甲'), []), {
    total: 24,
    filmed: 0,
    percent: 0,
    label: '总进度 0% · 已成片 0/24 集',
    episodeItems: [],
  })
})

test('episode progress unifies full 6-stage chain monotonically without reverting between script and film', () => {
  const step0 = [{ episode_id: 'e1', episode_no: 1, script_status: 'NONE', shot_total: 0 }]
  const step1 = [{ episode_id: 'e1', episode_no: 1, script_status: 'DRAFT', shot_total: 0 }]
  const step2 = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 0 }]
  const step3 = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 4, shot_frame_ready: 0, shot_ready: 0, shot_spoken_ready: 4 }]
  const step4 = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 4, shot_frame_ready: 2, shot_ready: 0, shot_spoken_ready: 4 }]
  const step5 = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 4, shot_frame_ready: 4, shot_ready: 2, shot_spoken_ready: 4 }]
  const step6 = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 4, shot_frame_ready: 4, shot_ready: 4, shot_spoken_ready: 4, compose_status: 'NONE' }]
  const step7 = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 4, shot_frame_ready: 4, shot_ready: 4, shot_spoken_ready: 4, compose_status: 'READY' }]

  const looksPartial = { outline_count: 1, look_total: 4, look_locked: 2 }
  const looksDone = { outline_count: 1, look_total: 4, look_locked: 4 }

  const p0 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step0, { outline_count: 0 }).percent
  const p1 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step1, { outline_count: 1 }).percent
  const p2 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step2, looksPartial).percent
  const p3 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step2, looksDone).percent
  const p4 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step3, looksDone).percent
  const p5 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step4, looksDone).percent
  const p6 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step5, looksDone).percent
  const p7 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step6, looksDone).percent
  const p8 = episodeProgress({ work_id: 'w1', episode_count: 1 }, step7, looksDone).percent

  assert.ok(p0 < p1 && p1 < p2 && p2 < p3 && p3 < p4 && p4 < p5 && p5 < p6 && p6 < p7 && p7 < p8)
  assert.equal(p0, 0)
  assert.equal(p8, 100)

  const multi = [
    { episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 4, shot_frame_ready: 4, shot_ready: 4, compose_status: 'READY', review_status: 'APPROVED' },
    { episode_id: 'e2', episode_no: 2, script_status: 'READY', shot_total: 4, shot_frame_ready: 4, shot_ready: 2, compose_status: 'NONE', review_status: 'NOT_SUBMITTED' },
    { episode_id: 'e3', episode_no: 3, script_status: 'NONE', shot_total: 0, compose_status: 'NONE', review_status: 'NOT_SUBMITTED' },
  ]
  const multiProg = episodeProgress({ work_id: 'w1', episode_count: 3 }, multi, { outline_count: 3, look_total: 2, look_locked: 2 })
  assert.equal(multiProg.total, 3)
  assert.equal(multiProg.filmed, 1)
  assert.equal(multiProg.percent, 60)
  assert.equal(multiProg.label, '总进度 60% · 已成片 1/3 集')
  assert.deepEqual(
    multiProg.episodeItems.map((it) => it.text),
    ['第1集：已成片', '第2集：视频 2/4', '第3集：待写剧本'],
  )
})

test('stale compose_status=READY with missing shot videos falls back to actual upstream stage', () => {
  const ep = {
    episode_id: 'e1',
    episode_no: 1,
    script_status: 'READY',
    storyboard_status: 'DRAFT',
    compose_status: 'READY',
    review_status: 'NOT_SUBMITTED',
    shot_total: 5,
    shot_frame_ready: 0,
    shot_ready: 0,
    shot_spoken_ready: 5,
    film_expired_flag: 0,
  }
  const w = { work_id: 'w-lamp', work_title: '最后一盏灯', work_status: 'COMPOSING', work_status_name: '合成中', episode_count: 1 }
  const detail = { outline_count: 1, look_total: 2, look_locked: 2 }

  assert.equal(resolveEpisodeStage(ep, detail).key, 'frame')
  const prog = episodeProgress(w, [ep], detail)
  assert.equal(prog.filmed, 0)
  assert.equal(prog.percent, 50)
  assert.equal(prog.label, '总进度 50% · 已成片 0/1 集')
  assert.equal(prog.episodeItems[0].text, '第1集：首帧 0/5')
  assert.equal(workCardStatus(w, [ep], detail), '制作中')
  assert.equal(continuePath(w, [ep], detail), '/works/w-lamp/storyboard?episode_id=e1')
  assert.equal(continueLabel(w, [ep], detail), '去出首帧')
})

test('expired film is not counted as filmed even if compose_status=READY', () => {
  const ep = {
    episode_id: 'e1',
    episode_no: 1,
    script_status: 'READY',
    shot_total: 4,
    shot_frame_ready: 4,
    shot_ready: 4,
    compose_status: 'READY',
    film_expired_flag: 1,
  }
  const stage = resolveEpisodeStage(ep, { outline_count: 1, look_total: 2, look_locked: 2 })
  assert.equal(stage.key, 'compose')
  assert.equal(stage.stageLabel, '待合成')
  assert.equal(stage.done, false)
})

test('single finished episode awaiting submission shows 已成片 instead of 合成中', () => {
  const ep = {
    episode_id: 'e1',
    episode_no: 1,
    script_status: 'READY',
    shot_total: 6,
    shot_frame_ready: 6,
    shot_ready: 6,
    compose_status: 'READY',
    review_status: 'NOT_SUBMITTED',
    film_expired_flag: 0,
  }
  const w = { work_id: 'w-chang', work_title: '长安灯火录', work_status: 'COMPOSING', work_status_name: '合成中', episode_count: 1 }
  assert.equal(workCardStatus(w, [ep], { outline_count: 1, look_total: 2, look_locked: 2 }), '已成片')
  assert.equal(episodeProgress(w, [ep]).label, '总进度 100% · 已成片 1/1 集')
  assert.equal(continueLabel(w, [ep]), '去看成片')
})

test('episode list keeps one row per episode instead of folding ranges', () => {
  const eps = []
  for (let i = 1; i <= 24; i += 1) {
    if (i <= 6) {
      eps.push({
        episode_id: `e${i}`,
        episode_no: i,
        script_status: 'READY',
        shot_total: 5,
        shot_frame_ready: 5,
        shot_ready: 5,
        compose_status: 'READY',
      })
    } else if (i === 7) {
      eps.push({
        episode_id: 'e7',
        episode_no: 7,
        script_status: 'READY',
        shot_total: 8,
        shot_frame_ready: 8,
        shot_ready: 3,
        compose_status: 'NONE',
      })
    } else if (i === 8) {
      eps.push({
        episode_id: 'e8',
        episode_no: 8,
        script_status: 'DRAFT',
        shot_total: 0,
        compose_status: 'NONE',
      })
    } else {
      eps.push({
        episode_id: `e${i}`,
        episode_no: i,
        script_status: 'NONE',
        shot_total: 0,
        compose_status: 'NONE',
      })
    }
  }
  const w = work('w1', '退婚三年后她成了首富', { episode_count: 24 })
  const detail = { outline_count: 24, look_total: 4, look_locked: 4 }
  const items = episodeStageItems(w, eps, detail)
  assert.equal(items.length, 24)
  assert.deepEqual(items.map((it) => it.no), Array.from({ length: 24 }, (_, idx) => idx + 1))
  assert.deepEqual(items[0], {
    key: 'ep-e1',
    no: 1,
    noLabel: '第1集',
    stageLabel: '已成片',
    text: '第1集：已成片',
    tone: 'done',
    to: '/works/w1/compose?episode_id=e1',
  })
  assert.equal(items[5].text, '第6集：已成片')
  assert.equal(items[6].noLabel, '第7集')
  assert.equal(items[6].stageLabel, '视频 3/8')
  assert.equal(items[6].tone, 'active')
  assert.equal(items[6].to, '/works/w1/storyboard?episode_id=e7')
  assert.equal(items[7].text, '第8集：剧本')
  assert.equal(items[23].noLabel, '第24集')
  assert.equal(items[23].stageLabel, '待写剧本')
  assert.equal(items[23].tone, 'todo')
  assert.equal(items[23].to, '/works/w1/script?episode_id=e24')
  assert.ok(items.every((it) => !/第\d+-\d+集/.test(it.text) && !it.text.includes('其余')))
  assert.equal(continuePath(w, eps, detail), '/works/w1/storyboard?episode_id=e7')
  assert.equal(continueLabel(w, eps, detail), '第7集 · 去生成视频')
})

test('episode list is never truncated, even for very long dramas', () => {
  const eps = Array.from({ length: 80 }, (_, idx) => ({
    episode_id: `e${idx + 1}`,
    episode_no: idx + 1,
    script_status: 'NONE',
    shot_total: 0,
    compose_status: 'NONE',
  }))
  const items = episodeStageItems(
    { work_id: 'w-long', episode_count: 80 },
    eps,
    { outline_count: 80, look_total: 0, look_locked: 0 },
  )
  assert.equal(items.length, 80)
  assert.equal(items[79].noLabel, '第80集')
  assert.equal(items[79].text, '第80集：待写剧本')
  assert.equal(items[79].to, '/works/w-long/script?episode_id=e80')
})

function mixedEpisodes(count) {
  const eps = []
  for (let i = 1; i <= count; i += 1) {
    if (i <= 6) {
      eps.push({
        episode_id: `e${i}`, episode_no: i, script_status: 'READY',
        shot_total: 5, shot_frame_ready: 5, shot_ready: 5, compose_status: 'READY',
      })
    } else if (i === 7) {
      eps.push({
        episode_id: 'e7', episode_no: 7, script_status: 'READY',
        shot_total: 8, shot_frame_ready: 8, shot_ready: 3, compose_status: 'NONE',
      })
    } else if (i === 8) {
      eps.push({ episode_id: 'e8', episode_no: 8, script_status: 'DRAFT', shot_total: 0, compose_status: 'NONE' })
    } else {
      eps.push({ episode_id: `e${i}`, episode_no: i, script_status: 'NONE', shot_total: 0, compose_status: 'NONE' })
    }
  }
  return eps
}

test('card summary folds long episode lists to the next episode plus counts', () => {
  const w = work('w1', '退婚三年后她成了首富', { episode_count: 24 })
  const eps = mixedEpisodes(24)
  const detail = { outline_count: 24, look_total: 4, look_locked: 4 }
  const items = episodeStageItems(w, eps, detail)
  const summary = episodeCardSummary(items, continuePath(w, eps, detail))
  assert.equal(CARD_EPISODE_INLINE_LIMIT, 4)
  assert.equal(summary.total, 24)
  assert.equal(summary.folded, true)
  assert.deepEqual(summary.counts, { done: 6, active: 2, todo: 16 })
  assert.equal(summary.countsLabel, '已成片 6 · 制作中 2 · 待写剧本 16')
  assert.equal(summary.nextItem.noLabel, '第7集')
  assert.deepEqual(summary.inlineItems.map((it) => it.noLabel), ['第7集'])
  // Without a continue target the first unfinished episode is shown.
  assert.equal(episodeCardSummary(items).nextItem.noLabel, '第7集')
  // The full list is untouched: every episode stays reachable from the modal.
  assert.equal(items.length, 24)
})

test('card summary keeps short lists fully inline at and below the limit', () => {
  for (const count of [1, 3, CARD_EPISODE_INLINE_LIMIT]) {
    const eps = mixedEpisodes(count)
    const items = episodeStageItems(
      { work_id: 'w1', episode_count: count }, eps, { outline_count: count, look_total: 4, look_locked: 4 },
    )
    const summary = episodeCardSummary(items)
    assert.equal(summary.folded, false)
    assert.equal(summary.inlineItems.length, count)
  }
  const five = episodeStageItems(
    { work_id: 'w1', episode_count: 5 }, mixedEpisodes(5), { outline_count: 5, look_total: 4, look_locked: 4 },
  )
  assert.equal(episodeCardSummary(five).folded, true)
})

test('card summary shows no next row when every episode is filmed', () => {
  const eps = Array.from({ length: 12 }, (_, idx) => ({
    episode_id: `e${idx + 1}`, episode_no: idx + 1, script_status: 'READY',
    shot_total: 4, shot_frame_ready: 4, shot_ready: 4, compose_status: 'READY',
  }))
  const items = episodeStageItems(
    { work_id: 'w-done', episode_count: 12 }, eps, { outline_count: 12, look_total: 2, look_locked: 2 },
  )
  const summary = episodeCardSummary(items, '/works/w-done/compose?episode_id=e1')
  assert.equal(summary.folded, true)
  assert.equal(summary.nextItem, null)
  assert.deepEqual(summary.inlineItems, [])
  assert.equal(summary.countsLabel, '已成片 12 · 制作中 0 · 待写剧本 0')
  assert.deepEqual(episodeCardSummary(null), {
    total: 0,
    folded: false,
    nextItem: null,
    inlineItems: [],
    counts: { done: 0, active: 0, todo: 0 },
    countsLabel: '已成片 0 · 制作中 0 · 待写剧本 0',
  })
})

test('findEpisodeItem locates an episode by number and rejects bad input', () => {
  const eps = Array.from({ length: 80 }, (_, idx) => ({
    episode_id: `e${idx + 1}`, episode_no: idx + 1, script_status: 'NONE', shot_total: 0, compose_status: 'NONE',
  }))
  const items = episodeStageItems(
    { work_id: 'w-long', episode_count: 80 }, eps, { outline_count: 80, look_total: 0, look_locked: 0 },
  )
  assert.equal(findEpisodeItem(items, '57').noLabel, '第57集')
  assert.equal(findEpisodeItem(items, ' 57 ').no, 57)
  assert.equal(findEpisodeItem(items, '第57集').no, 57)
  assert.equal(findEpisodeItem(items, 57).no, 57)
  assert.equal(findEpisodeItem(items, '1').no, 1)
  assert.equal(findEpisodeItem(items, '80').no, 80)
  assert.equal(findEpisodeItem(items, '81'), null)
  assert.equal(findEpisodeItem(items, '0'), null)
  assert.equal(findEpisodeItem(items, '-3'), null)
  assert.equal(findEpisodeItem(items, 'abc'), null)
  assert.equal(findEpisodeItem(items, ''), null)
  assert.equal(findEpisodeItem(items, null), null)
  assert.equal(findEpisodeItem(null, '1'), null)
})

test('unlocked looks route to character-bible with 去定妆', () => {
  const w = { work_id: 'w1', work_title: '甲', episode_count: 1 }
  const eps = [{ episode_id: 'e1', episode_no: 1, script_status: 'READY', shot_total: 0 }]
  const detail = { outline_count: 1, look_total: 4, look_locked: 1 }
  assert.equal(continuePath(w, eps, detail), '/works/w1/characters?episode_id=e1')
  assert.equal(continueLabel(w, eps, detail), '去定妆')
  assert.equal(episodeStageItems(w, eps, detail)[0].text, '第1集：定妆 1/4')
})

test('continue opens the step that is actually next', () => {
  assert.equal(continuePath(work('w1', '甲'), []), '/works/w1/script')
  assert.equal(continuePath(work('w1', '甲'), [
    { episode_id: 'e2', episode_no: 2, review_status: 'PENDING', script_status: 'READY', compose_status: 'READY' },
  ]), '/works/w1/compose?episode_id=e2')
  assert.equal(continuePath(work('w1', '甲'), [
    { episode_id: 'e1', episode_no: 1, review_status: 'APPROVED', script_status: 'READY' },
    { episode_id: 'e3', episode_no: 3, review_status: 'NOT_SUBMITTED', script_status: 'READY', storyboard_status: 'DRAFT' },
  ]), '/works/w1/storyboard?episode_id=e3')
  assert.equal(continuePath(work('w1', '甲'), [
    { episode_id: 'e4', episode_no: 4, review_status: 'NOT_SUBMITTED', script_status: 'READY', storyboard_status: 'FAILED' },
  ]), '/works/w1/storyboard?episode_id=e4')
  assert.equal(continueLabel(work('w1', '甲'), []), '去写剧本')
  assert.equal(continueLabel(work('w1', '甲'), [
    { episode_id: 'e3', episode_no: 3, review_status: 'NOT_SUBMITTED', script_status: 'READY', storyboard_status: 'DRAFT' },
  ]), '第3集 · 去生成视频')
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
  assert.equal(nextStepText({ loadError: true, works: [], attention: [] }), '作品列表加载失败')
  assert.equal(nextStepText({ works: [], attention: [] }), '暂无作品')
  assert.equal(nextStepText({
    works: [work('w1', '悬疑法医手记')],
    attention: [],
  }), '最近作品：《悬疑法医手记》')
  assert.match(nextStepText({
    works: [work('w1', '悬疑法医手记')],
    attention: [{ kind: 'generate_failed', workTitle: '悬疑法医手记' }],
  }), /生成失败/)
  assert.match(nextStepText({
    works: [work('w1', '甲')],
    attention: [{ kind: 'review_pending', workTitle: '甲' }],
  }), /待审核/)
})
