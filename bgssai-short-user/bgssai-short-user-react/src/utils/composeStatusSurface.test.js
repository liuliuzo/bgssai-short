import test from 'node:test'
import assert from 'node:assert/strict'
import {
  claimsReadyPass,
  composeSurface,
  composeSurfaceFromEntry,
  episodeComposeTag,
  episodeSelectLabel,
} from './composeStatusSurface.js'

const episode = {
  episode_no: 1,
  episode_title: '归来',
  script_status_name: '已就绪',
  review_status_name: '已通过',
  compose_status: 'READY',
  compose_status_name: '已就绪',
}

test('unsettled compose status does not claim ready or not-ready', () => {
  const surface = composeSurface({ settled: false })
  const label = episodeSelectLabel(episode, { onCompose: true, surface })
  const tag = episodeComposeTag(episode, { onCompose: true, surface })
  assert.equal(surface.paneText, '合成状态 加载中 / 预检 核对中 / 风险 -')
  assert.equal(surface.chipText, '核对中')
  assert.equal(label, '第1集 归来（核对中）')
  assert.equal(tag, '核对中')
  assert.equal(claimsReadyPass(surface.paneText), false)
  assert.equal(claimsReadyPass(label), false)
  assert.equal(claimsReadyPass(tag), false)
  assert.equal(surface.paneText.includes('未就绪'), false)
})

test('settled ready preflight restores the ready/pass chip and the pane together', () => {
  const surface = composeSurface({
    settled: true,
    film: { compose_status: 'READY', expired_flag: 0 },
    precheck: { ready: true, risk_level: 'PASS' },
  })
  const label = episodeSelectLabel(episode, { onCompose: true, surface })
  const tag = episodeComposeTag(episode, { onCompose: true, surface })
  assert.equal(surface.paneText, '合成状态 READY / 预检 可合成 / 风险 PASS')
  assert.equal(label, '第1集 归来（已就绪/已通过）')
  assert.equal(tag, '已就绪')
  assert.equal(surface.allowReadyPassChip, true)
})

test('coarse episode READY does not keep a ready chip when live preflight is not ready', () => {
  const surface = composeSurface({
    settled: true,
    film: { compose_status: 'READY' },
    precheck: { ready: false, risk_level: 'WARN' },
  })
  const label = episodeSelectLabel(episode, { onCompose: true, surface })
  const tag = episodeComposeTag(episode, { onCompose: true, surface })
  assert.equal(surface.paneText, '合成状态 READY / 预检 未就绪 / 风险 WARN')
  assert.equal(label, '第1集 归来（未就绪）')
  assert.equal(tag, '未就绪')
  assert.equal(claimsReadyPass(label), false)
  assert.equal(claimsReadyPass(tag), false)
})

test('failed status fetch is not shown as not-ready or passed', () => {
  const surface = composeSurface({ settled: true, error: true })
  const label = episodeSelectLabel(episode, { onCompose: true, surface })
  assert.equal(surface.chipText, '未能核对')
  assert.equal(surface.paneText.includes('未就绪'), false)
  assert.equal(claimsReadyPass(label), false)
})

test('another episode or a tab outside compose keeps the stored script and review label', () => {
  const label = episodeSelectLabel(episode, { onCompose: false })
  const tag = episodeComposeTag(episode, { onCompose: false })
  assert.equal(label, '第1集 归来（已就绪/已通过）')
  assert.equal(tag, '已就绪')
})

test('a missing episode entry stays unsettled for both chip and pane', () => {
  const surface = composeSurfaceFromEntry(undefined)
  const label = episodeSelectLabel(episode, { onCompose: true, surface })
  assert.equal(surface.phase, 'loading')
  assert.equal(surface.paneText.includes('未就绪'), false)
  assert.equal(claimsReadyPass(label), false)
})
