/**
 * Show the stored shot number. Do not invent one from an opaque shot id.
 * If the referenced shot is gone, say so instead of implying it is still in the episode.
 */
export function continuityShotLabel(issue, episodeShotNos) {
  if (!issue) return '—'
  const present = issue.shot_in_episode === 1 || issue.shot_in_episode === true
  const no = issue.shot_no
  if (!present || no == null || no === '') return '引用的镜头已不在本集'
  const number = Number(no)
  if (Array.isArray(episodeShotNos) && episodeShotNos.length && !episodeShotNos.map(Number).includes(number)) {
    return `镜 ${number}（本集当前镜头里没有这一镜）`
  }
  return `镜 ${number}`
}
