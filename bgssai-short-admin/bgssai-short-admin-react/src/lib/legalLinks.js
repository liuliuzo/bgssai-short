/**
 * Official legal pages live only on bgssai-website.
 * Paths match bgssai-website docs/legal/README.md EXACTLY.
 * Outbound links to official legal documents on bgssai-website.
 * Approved ICP filing for bgssai-short.com: 苏ICP备2024070062号-15.
 * Do not invent other ICP / license numbers.
 *
 * Product UI (footer + login consent) lists only terms + privacy (Standards §15).
 * Inventory / sharing / permissions stay on the website legal hub, not in-product.
 * Keep in sync with bgssai-short-user-react/src/lib/legalLinks.js.
 */


export const WEBSITE_ORIGIN = 'https://www.bgssai.com'

export const COPYRIGHT_HOLDER = '昆山兵贵神速智能科技有限公司'
export const COPYRIGHT_YEAR = new Date().getFullYear()
export const COPYRIGHT_LINE = `© ${COPYRIGHT_YEAR} ${COPYRIGHT_HOLDER}`

export const ICP_FILING_NUMBER = '苏ICP备2024070062号-15'
export const ICP_FILING_URL = 'https://beian.miit.gov.cn/'

/** Official pages shown in product UI. label_* is consent copy; footer_* is bilingual footer. */
export const LEGAL_PAGES = Object.freeze([
  {
    key: 'terms',
    slug: 'terms-of-service',
    path_zh: '/terms-of-service/',
    path_en: '/en/terms-of-service/',
    label_zh: '用户协议',
    label_en: 'Terms',
    footer_zh: '服务条款',
    footer_en: 'Terms of Service',
  },
  {
    key: 'privacy',
    slug: 'privacy-policy',
    path_zh: '/privacy-policy/',
    path_en: '/en/privacy-policy/',
    label_zh: '隐私政策',
    label_en: 'Privacy',
    footer_zh: '隐私政策',
    footer_en: 'Privacy Policy',
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
  const normalized = String(text || '').replaceAll(ICP_FILING_NUMBER, '')
  return LICENSE_NUMBER_RE.test(normalized)
}

export function hasForbiddenLegalPath(text) {
  return FORBIDDEN_PATH_RE.test(String(text || ''))
}
