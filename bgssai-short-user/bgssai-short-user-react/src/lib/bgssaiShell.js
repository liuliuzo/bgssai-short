/**
 * Bot / Chat 窄栏检测（对照产品线 MOBILE-IN-BOT-SHELL.md）。
 * 只认查询参数 bgssai_shell / chat_pane，不把窗口宽度当成壳模式。
 * 参数只改布局，不改鉴权，不携带密钥。
 */

export const SHELL_STORAGE_KEY = 'bgssai_shell'
export const SHELL_QUERY_KEYS = ['bgssai_shell', 'chat_pane']

const TRUE_VALUES = new Set(['1', 'true', 'yes'])
const FALSE_VALUES = new Set(['0', 'false', 'no'])

export function isTruthyFlag(value) {
  if (value == null) return false
  return TRUE_VALUES.has(String(value).trim().toLowerCase())
}

export function isFalsyFlag(value) {
  if (value == null) return false
  return FALSE_VALUES.has(String(value).trim().toLowerCase())
}

export function readFlagsFromSearch(search) {
  const raw = String(search || '')
  const query = raw.startsWith('?') ? raw.slice(1) : raw
  if (!query) return null
  const params = new URLSearchParams(query)
  let seenTrue = false
  let seenFalse = false
  for (const key of SHELL_QUERY_KEYS) {
    if (!params.has(key)) continue
    const value = params.get(key)
    if (isTruthyFlag(value)) seenTrue = true
    if (isFalsyFlag(value)) seenFalse = true
  }
  if (seenTrue) return true
  if (seenFalse) return false
  return null
}

export function readFlagsFromLocation(locationLike) {
  const search = locationLike && locationLike.search ? locationLike.search : ''
  const fromSearch = readFlagsFromSearch(search)
  if (fromSearch !== null) return fromSearch

  const hash = locationLike && locationLike.hash ? locationLike.hash : ''
  const qIndex = hash.indexOf('?')
  if (qIndex >= 0) {
    const fromHash = readFlagsFromSearch(hash.slice(qIndex + 1))
    if (fromHash !== null) return fromHash
  }
  return null
}

export function readPersistedShell(storage) {
  try {
    return storage && storage.getItem(SHELL_STORAGE_KEY) === '1'
  } catch {
    return false
  }
}

export function persistShell(active, storage) {
  try {
    if (!storage) return
    if (active) storage.setItem(SHELL_STORAGE_KEY, '1')
    else storage.removeItem(SHELL_STORAGE_KEY)
  } catch {
    // sessionStorage 不可用时仍允许当次布局生效
  }
}

export function resolveShellMode(locationLike, storage) {
  const fromUrl = readFlagsFromLocation(locationLike)
  if (fromUrl === true) return { active: true, persist: true }
  if (fromUrl === false) return { active: false, persist: true }
  return { active: readPersistedShell(storage), persist: false }
}

export function applyBgssaiShell(active, doc, storage) {
  const root = doc && doc.documentElement
  if (root) {
    if (active) root.setAttribute('data-bgssai-shell', '1')
    else root.removeAttribute('data-bgssai-shell')
  }
  const body = doc && doc.body
  if (body) {
    body.classList.toggle('bgssai-shell', Boolean(active))
  }
  persistShell(active, storage)
  return Boolean(active)
}

export function syncBgssaiShell(locationLike, storage, doc) {
  const resolved = resolveShellMode(locationLike, storage)
  applyBgssaiShell(resolved.active, doc, storage)
  return resolved.active
}
