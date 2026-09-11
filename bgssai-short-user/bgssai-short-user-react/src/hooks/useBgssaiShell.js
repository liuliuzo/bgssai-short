import { useEffect, useState } from 'react'
import { useLocation } from 'react-router-dom'
import { resolveShellMode, applyBgssaiShell } from '../lib/bgssaiShell'

export function useBgssaiShell() {
  const location = useLocation()
  const [active, setActive] = useState(() => {
    if (typeof window === 'undefined') return false
    return resolveShellMode(window.location, window.sessionStorage).active
  })

  useEffect(() => {
    if (typeof window === 'undefined') return
    const next = resolveShellMode(window.location, window.sessionStorage).active
    applyBgssaiShell(next, document, window.sessionStorage)
    setActive(next)
  }, [location.search, location.hash, location.pathname])

  return active
}
