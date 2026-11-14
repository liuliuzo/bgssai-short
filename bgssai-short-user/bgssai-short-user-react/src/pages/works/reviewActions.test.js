import test from 'node:test'
import assert from 'node:assert/strict'
import { reviewActionState } from './reviewActions.js'

test('review actions follow the actual status and are never both enabled', () => {
  const idle = reviewActionState({ reviewStatus: 'NOT_SUBMITTED', composeReady: true })
  assert.equal(idle.submit, true)
  assert.equal(idle.withdraw, false)

  const pending = reviewActionState({ reviewStatus: 'PENDING', composeReady: true })
  assert.equal(pending.submit, false)
  assert.equal(pending.withdraw, true)

  const approved = reviewActionState({ reviewStatus: 'APPROVED', composeReady: true })
  assert.equal(approved.submit, false)
  assert.equal(approved.withdraw, false)

  const noFilm = reviewActionState({ reviewStatus: 'NOT_SUBMITTED', composeReady: false })
  assert.equal(noFilm.submit, false)
  assert.equal(noFilm.withdraw, false)

  for (const status of ['NOT_SUBMITTED', 'PENDING', 'APPROVED', 'REJECTED', '']) {
    const state = reviewActionState({ reviewStatus: status, composeReady: true })
    assert.equal(state.submit && state.withdraw, false)
  }
})
