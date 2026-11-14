import test from 'node:test'
import assert from 'node:assert/strict'
import { continuityShotLabel } from './continuityShotLabel.js'

test('continuity uses the stored shot number and admits a missing shot', () => {
  assert.equal(continuityShotLabel({
    shot_id: 'sh-a1b2c3d4e5f3',
    shot_no: 1,
    shot_in_episode: 1,
  }, [1]), '镜 1')
  assert.equal(continuityShotLabel({
    shot_id: 'sh-a1b2c3d4e5f3',
    shot_no: 3,
    shot_in_episode: 0,
  }, [1]), '引用的镜头已不在本集')
  assert.equal(continuityShotLabel({
    shot_id: 'sh-a1b2c3d4e5f3',
    shot_no: 3,
    shot_in_episode: 1,
  }, [1]), '镜 3（本集当前镜头里没有这一镜）')
})
