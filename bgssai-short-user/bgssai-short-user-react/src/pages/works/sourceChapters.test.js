import assert from 'node:assert/strict'
import { describe, it } from 'node:test'
import {
  canMergeChapterWithNext,
  findChapterIndex,
  mergeChapterWithNext,
  updateChapterTitle,
} from './sourceChapters.js'

function makeChapters(count) {
  return Array.from({ length: count }, (_, i) => ({
    chapter_id: `ch-${i + 1}`,
    chapter_no: i + 1,
    chapter_title: `第${i + 1}章`,
    chapter_text: `正文${i + 1}`,
    char_count: 10,
  }))
}

describe('sourceChapters', () => {
  it('locates a second-page row by chapter_id, not by page-local position', () => {
    const chapters = makeChapters(25)
    // First row rendered on page 2 carries chapter 21 identity.
    const pageTwoFirstRow = chapters[20]
    assert.equal(findChapterIndex(chapters, pageTwoFirstRow), 20)
  })

  it('updates the title of the intended chapter only', () => {
    const chapters = makeChapters(25)
    const next = updateChapterTitle(chapters, chapters[20], '新标题')
    assert.equal(next[20].chapter_title, '新标题')
    assert.equal(next[0].chapter_title, '第1章')
    assert.equal(chapters[20].chapter_title, '第21章')
  })

  it('merges the intended chapter with its own next chapter', () => {
    const chapters = makeChapters(25)
    const next = mergeChapterWithNext(chapters, chapters[21])
    assert.equal(next.length, 24)
    assert.equal(next[21].chapter_text, '正文22\n\n正文23')
    assert.equal(next[21].char_count, 20)
    assert.equal(next[0].chapter_text, '正文1')
  })

  it('refuses to merge the last chapter and reports it as not mergeable', () => {
    const chapters = makeChapters(3)
    assert.equal(canMergeChapterWithNext(chapters, chapters[2]), false)
    assert.equal(canMergeChapterWithNext(chapters, chapters[1]), true)
    assert.equal(mergeChapterWithNext(chapters, chapters[2]), chapters)
  })

  it('keeps identity after a merge removed a following chapter', () => {
    const chapters = makeChapters(5)
    const merged = mergeChapterWithNext(chapters, chapters[1])
    const renamed = updateChapterTitle(merged, merged[2], '改名章')
    assert.equal(renamed[2].chapter_id, 'ch-4')
    assert.equal(renamed[2].chapter_title, '改名章')
    assert.equal(renamed[1].chapter_title, '第2章')
  })

  it('returns -1 for an unknown row', () => {
    const chapters = makeChapters(3)
    assert.equal(findChapterIndex(chapters, { chapter_id: 'missing', chapter_no: 99 }), -1)
    assert.equal(updateChapterTitle(chapters, { chapter_id: 'missing' }, 'x'), chapters)
  })
})
