/** Only same-site relative paths may be used after login. */
export function safeReturnPath(raw) {
  if (typeof raw !== 'string') return null
  if (!raw.startsWith('/') || raw.startsWith('//') || raw.includes('\\') || raw.includes('\0')) return null
  if (raw.startsWith('/login') || raw.startsWith('/oauth-callback')) return null
  return raw
}

export function loginHref(extra) {
  const params = new URLSearchParams()
  if (extra) {
    Object.entries(extra).forEach(([key, value]) => {
      if (value != null && value !== '') params.set(key, value)
    })
  }
  const path = `${window.location.pathname}${window.location.search}`
  if (!window.location.pathname.startsWith('/login') && window.location.pathname !== '/oauth-callback') {
    params.set('redirect', path)
  }
  const query = params.toString()
  return query ? `/login?${query}` : '/login'
}
