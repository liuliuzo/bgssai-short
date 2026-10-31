/** Chinese-only product. `/en` is not a second locale. */
export const CHINESE_ENTRY_PATH = '/login'

export function isEnglishPrefix(pathname) {
  return pathname === '/en' || pathname === '/en/' || pathname.startsWith('/en/')
}

/** Guest-usable Chinese login, preserving the original query and hash. */
export function englishPrefixTarget(pathname, search = '', hash = '') {
  if (!isEnglishPrefix(pathname)) return null
  return `${CHINESE_ENTRY_PATH}${search}${hash}`
}
