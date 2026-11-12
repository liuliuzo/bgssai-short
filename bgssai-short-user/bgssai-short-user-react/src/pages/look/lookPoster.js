/** Sample points inside a clip. The first moments are often motion blur. */
export function posterTimes(duration) {
  const marks = [0.12, 0.45, 0.9, 1.6, 2.4]
  if (!Number.isFinite(duration) || duration <= 0.2) return [0]
  const last = Math.max(0, duration - 0.08)
  const picked = marks.filter((mark) => mark <= last)
  if (!picked.length) picked.push(Math.min(0.12, last))
  return picked
}

/** Higher means sharper edges. A smeared video frame scores near a flat image. */
export function laplacianScore(image) {
  if (!image || !image.data || image.width < 3 || image.height < 3) return 0
  const { data, width, height } = image
  const lumAt = (x, y) => {
    const i = (y * width + x) * 4
    return data[i] * 0.299 + data[i + 1] * 0.587 + data[i + 2] * 0.114
  }
  let acc = 0
  let n = 0
  for (let y = 1; y < height - 1; y += 1) {
    for (let x = 1; x < width - 1; x += 1) {
      const lap = lumAt(x - 1, y) + lumAt(x + 1, y) + lumAt(x, y - 1) + lumAt(x, y + 1) - 4 * lumAt(x, y)
      acc += lap * lap
      n += 1
    }
  }
  return n ? acc / n : 0
}
