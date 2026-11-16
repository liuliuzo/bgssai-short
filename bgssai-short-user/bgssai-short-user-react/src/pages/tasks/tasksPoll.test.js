import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import { hasActiveTasks } from './tasksPoll.js'

describe('tasksPoll', () => {
  it('polls while any visible task is queued or running', () => {
    assert.equal(hasActiveTasks([{ task_status: 'QUEUED' }]), true)
    assert.equal(hasActiveTasks([{ task_status: 'SUCCEEDED' }, { task_status: 'RUNNING' }]), true)
  })

  it('stops when every visible task has settled', () => {
    assert.equal(hasActiveTasks([
      { task_status: 'SUCCEEDED' },
      { task_status: 'FAILED' },
      { task_status: 'CANCELED' },
    ]), false)
    assert.equal(hasActiveTasks([]), false)
    assert.equal(hasActiveTasks(null), false)
  })
})
