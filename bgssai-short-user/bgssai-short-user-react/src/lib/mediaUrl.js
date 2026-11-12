/** Seed portraits and shot stand-ins are stored as SVG data URLs, not photos or video files. */

export function isPlaceholderMediaUrl(url) {
  const value = String(url || '').trim().toLowerCase()
  if (!value) return false
  return value.startsWith('data:image/svg') || value.includes('image/svg+xml')
}

export function isRealPhotoUrl(url) {
  const value = String(url || '').trim()
  return value !== '' && !isPlaceholderMediaUrl(value)
}
