import test from 'node:test'
import assert from 'node:assert/strict'
import { CREATE_WORK_DEFAULTS, createWorkSettingLine } from './workCreateDefaults.js'

test('a new work starts as a short trial, and the screen says so', () => {
  assert.equal(CREATE_WORK_DEFAULTS.episode_count, 3)
  assert.equal(CREATE_WORK_DEFAULTS.episode_duration_sec, 60)
  const line = createWorkSettingLine(CREATE_WORK_DEFAULTS)
  assert.match(line, /3 集 × 60 秒/)
  assert.match(line, /24 集 × 90 秒/)
  assert.match(line, /复仇逆袭/)
})
