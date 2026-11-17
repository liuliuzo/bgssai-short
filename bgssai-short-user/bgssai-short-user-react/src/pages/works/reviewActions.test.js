import test from 'node:test'
import assert from 'node:assert/strict'
import { formatTubePublishHint, reviewActionState } from './reviewActions.js'

test('review actions follow the actual status and are never both enabled', () => {
  const idle = reviewActionState({ reviewStatus: 'NOT_SUBMITTED', composeReady: true })
  assert.equal(idle.submit, true)
  assert.equal(idle.withdraw, false)
  assert.equal(idle.mode, 'submit')
  assert.equal(idle.buttonLabel, '申请上架 Tube')
  assert.equal(idle.enabled, true)

  const pending = reviewActionState({ reviewStatus: 'PENDING', composeReady: true })
  assert.equal(pending.submit, false)
  assert.equal(pending.withdraw, true)
  assert.equal(pending.mode, 'withdraw')
  assert.equal(pending.buttonLabel, '撤回上架申请')
  assert.equal(pending.enabled, true)

  const approved = reviewActionState({ reviewStatus: 'APPROVED', composeReady: true })
  assert.equal(approved.submit, false)
  assert.equal(approved.withdraw, false)
  assert.equal(approved.mode, 'submit')
  assert.equal(approved.buttonLabel, '申请上架 Tube')
  assert.equal(approved.enabled, false)
  assert.equal(approved.submitReason, '这一集已通过上架审核')

  const noFilm = reviewActionState({ reviewStatus: 'NOT_SUBMITTED', composeReady: false })
  assert.equal(noFilm.submit, false)
  assert.equal(noFilm.withdraw, false)
  assert.equal(noFilm.enabled, false)
  assert.equal(noFilm.submitReason, '成片就绪后才能申请上架 Tube')

  for (const status of ['NOT_SUBMITTED', 'PENDING', 'APPROVED', 'REJECTED', '']) {
    const state = reviewActionState({ reviewStatus: status, composeReady: true })
    assert.equal(state.submit && state.withdraw, false)
  }
})

test('tube publish hints replace internal review_status codes with clear user copy', () => {
  assert.equal(
    formatTubePublishHint('剧集审核未通过（review_status=NOT_SUBMITTED，需 APPROVED）后才能发布到 Tube'),
    '尚未申请上架 Tube',
  )
  assert.equal(
    formatTubePublishHint('剧集审核未通过（review_status=PENDING，需 APPROVED）后才能发布到 Tube'),
    '上架申请审核中',
  )
  assert.equal(
    formatTubePublishHint('剧集审核未通过（review_status=REJECTED，需 APPROVED）后才能发布到 Tube'),
    '上架申请未通过',
  )
  assert.equal(formatTubePublishHint('未配置 Tube 令牌'), '未配置 Tube 令牌')
})
