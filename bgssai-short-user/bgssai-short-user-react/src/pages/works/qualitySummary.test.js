import test from 'node:test'
import assert from 'node:assert/strict'
import { qualityHeadline } from './qualitySummary.js'

test('quality tab shows stored scores and a plain empty state when none exist', () => {
  const empty = qualityHeadline({
    profile: { quality_threshold_score: null, overall_quality_score: null, gate_list: [] },
    gateResult: { gate_result: null },
    gateCode: 'G1_SCRIPT',
  })
  assert.equal(empty.kind, 'empty')
  assert.equal(empty.text.includes('-'), false)

  const ready = qualityHeadline({
    profile: { quality_threshold_score: 75, overall_quality_score: null, gate_list: [
      { gate_code: 'G1_SCRIPT', gate_status: 'PASSED', score: 92 },
    ] },
    gateResult: { gate_result: null },
    gateCode: 'G1_SCRIPT',
  })
  assert.equal(ready.kind, 'ready')
  assert.match(ready.text, /作品阈值 75/)
  assert.match(ready.text, /已通过 \/ 92/)
  assert.equal(ready.text.includes(' -'), false)
})
