import test from 'node:test'
import assert from 'node:assert/strict'
import {
  hasAnnouncedTaskOutcome,
  shouldAnnounceTaskOutcome,
  withoutOutlineTaskId,
} from './taskOutcome.js'

test('an outline task is announced once, then further reloads stay quiet', () => {
  const seen = new Set()
  const reloads = [1, 2, 3, 4]
  const announced = reloads.map(() => shouldAnnounceTaskOutcome(seen, 'outline-task'))
  assert.deepEqual(announced, [true, false, false, false])
  assert.equal(hasAnnouncedTaskOutcome(seen, 'outline-task'), true)
})

test('a different task id still announces, including the script task', () => {
  const seen = new Set()
  assert.equal(shouldAnnounceTaskOutcome(seen, 'outline-1'), true)
  assert.equal(shouldAnnounceTaskOutcome(seen, 'script-1'), true)
  assert.equal(shouldAnnounceTaskOutcome(seen, 'outline-1'), false)
  assert.equal(shouldAnnounceTaskOutcome(seen, 'script-1'), false)
})

test('numeric and string forms of the same task id collapse', () => {
  const seen = new Set()
  assert.equal(shouldAnnounceTaskOutcome(seen, 15), true)
  assert.equal(shouldAnnounceTaskOutcome(seen, '15'), false)
})

test('a blank task id is not announced and does not block the next one', () => {
  const seen = new Set()
  assert.equal(shouldAnnounceTaskOutcome(seen, null), false)
  assert.equal(shouldAnnounceTaskOutcome(seen, undefined), false)
  assert.equal(shouldAnnounceTaskOutcome(seen, ''), false)
  assert.equal(seen.size, 0)
  assert.equal(shouldAnnounceTaskOutcome(seen, 'outline-2'), true)
})

test('history state drops outlineTaskId and keeps unrelated keys', () => {
  assert.equal(withoutOutlineTaskId({ outlineTaskId: 'task-9' }), null)
  assert.deepEqual(
    withoutOutlineTaskId({ outlineTaskId: 'task-9', from: 'create' }),
    { from: 'create' },
  )
  const other = { from: 'create' }
  assert.equal(withoutOutlineTaskId(other), other)
  assert.equal(withoutOutlineTaskId(null), null)
  assert.equal(withoutOutlineTaskId(['outlineTaskId']), null)
})
