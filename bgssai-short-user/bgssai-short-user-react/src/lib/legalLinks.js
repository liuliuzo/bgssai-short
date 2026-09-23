/**
 * Official legal pages live only on bgssai-website.
 * Paths match bgssai-website docs/legal/README.md EXACTLY.
 * This module is outbound links. Status: 需法务审阅.
 * Do not invent ICP / license numbers.
 *
 * Login UI (aligned with bgssai-blog v003): consent = 用户协议 + 隐私政策 (zh);
 * login footer = copyright line only. App footer shows terms and privacy in the current language.
 */


export const WEBSITE_ORIGIN = 'https://www.bgssai.com'

export const COPYRIGHT_HOLDER = '昆山兵贵神速智能科技有限公司'
export const COPYRIGHT_YEAR = new Date().getFullYear()
export const COPYRIGHT_LINE = `© ${COPYRIGHT_YEAR} ${COPYRIGHT_HOLDER}`

/** Shorter bilingual labels (match blog) so the footer wraps cleanly with · separators. */
export const LEGAL_PAGES = Object.freeze([
  {
    key: 'terms',
    slug: 'terms-of-service',
    path_zh: '/terms-of-service/',
    path_en: '/en/terms-of-service/',
    label_zh: '用户协议',
    label_en: 'Terms',
  },
  {
    key: 'privacy',
    slug: 'privacy-policy',
    path_zh: '/privacy-policy/',
    path_en: '/en/privacy-policy/',
    label_zh: '隐私政策',
    label_en: 'Privacy',
  },
])

/** Login / register consent uses only these two pages (Chinese labels + Chinese URLs). */
export const LOGIN_CONSENT_PAGES = Object.freeze(
  LEGAL_PAGES.filter((p) => p.key === 'terms' || p.key === 'privacy'),
)

export const LEGAL_HUB = Object.freeze({
  slug: 'legal',
  path_zh: '/legal/',
  path_en: '/en/legal/',
  label_zh: '法律信息',
  label_en: 'Legal',
})

const LICENSE_NUMBER_RE = /ICP|备案|增值电信|经营许可|京网安|沪公网|苏ICP|许可证号/i

const FORBIDDEN_PATH_RE = /bgssai\.cn|\/legal\/user-agreement|children-privacy|personal-info-collection/i

export function legalPageUrl(page, locale = 'zh') {
  const path = locale === 'en' ? page.path_en : page.path_zh
  return `${WEBSITE_ORIGIN}${path}`
}

export function bilingualLabel(page) {
  return `${page.label_zh} / ${page.label_en}`
}

export function hasInventedLicenseNumber(text) {
  return LICENSE_NUMBER_RE.test(String(text || ''))
}

export function hasForbiddenLegalPath(text) {
  return FORBIDDEN_PATH_RE.test(String(text || ''))
}
