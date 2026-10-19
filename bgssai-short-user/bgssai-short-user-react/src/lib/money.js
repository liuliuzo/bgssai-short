export function yuanToCent(yuan) {
  const n = Number(yuan)
  if (!Number.isFinite(n)) return 0
  return Math.round(n * 100)
}
