/**
 * Chapter edits are located by the row's own identity, never by the
 * page-local render index: the chapter table paginates at 20 rows, so a
 * render index on page 2 does not address the same chapter in the full list.
 */

/**
 * @param {object[]} chapters full chapter list
 * @param {object|null|undefined} row table row (carries chapter_id / chapter_no)
 * @returns {number} index into chapters, or -1 when the row cannot be located
 */
export function findChapterIndex(chapters, row) {
  if (!Array.isArray(chapters) || !row) return -1
  if (row.chapter_id != null) {
    const byId = chapters.findIndex((c) => c.chapter_id === row.chapter_id)
    if (byId >= 0) return byId
  }
  if (row.chapter_no != null) {
    return chapters.findIndex((c) => c.chapter_no === row.chapter_no)
  }
  return -1
}

/**
 * @param {object[]} chapters
 * @param {object} row
 * @param {string} title
 * @returns {object[]} new list; unchanged reference when the row is not found
 */
export function updateChapterTitle(chapters, row, title) {
  const index = findChapterIndex(chapters, row)
  if (index < 0) return chapters
  const next = chapters.slice()
  next[index] = { ...next[index], chapter_title: title }
  return next
}

/**
 * Merge the located chapter with the following one.
 * @param {object[]} chapters
 * @param {object} row
 * @returns {object[]} new list; unchanged reference for the last / unknown row
 */
export function mergeChapterWithNext(chapters, row) {
  const index = findChapterIndex(chapters, row)
  if (index < 0 || index >= chapters.length - 1) return chapters
  const next = chapters[index + 1]
  const merged = chapters.slice()
  merged[index] = {
    ...merged[index],
    chapter_text: `${merged[index].chapter_text || ''}\n\n${next.chapter_text || ''}`,
    char_count: (merged[index].char_count || 0) + (next.char_count || 0),
  }
  merged.splice(index + 1, 1)
  return merged
}

/** @returns {boolean} whether the row has a following chapter to merge into */
export function canMergeChapterWithNext(chapters, row) {
  const index = findChapterIndex(chapters, row)
  return index >= 0 && index < chapters.length - 1
}
