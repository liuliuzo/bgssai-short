import test from 'node:test'
import assert from 'node:assert/strict'
import { formatWorkTime, workListMeta, workStudioPath } from './worksListUx.js'

test('works list shows the work id and updated time, and enters via a path', () => {
  assert.equal(workStudioPath('w-abc123def456'), '/works/w-abc123def456/script')
  const meta = workListMeta({
    work_id: 'w-abc123def456',
    work_title: '全链路验收短剧',
    updated_time: '2026-11-14T08:05:00',
  })
  assert.match(meta, /w-abc123def456/)
  assert.match(meta, /2026-11-14 08:05/)
  assert.equal(formatWorkTime(null), '')
  assert.equal(workListMeta({ work_title: '全链路验收短剧' }), '')
})
