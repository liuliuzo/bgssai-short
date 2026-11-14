/**
 * Official legal pages live only on bgssai-website.
 * Paths match bgssai-website docs/legal/README.md EXACTLY.
 * Product footers list five pages and the approved ICP filing for bgssai-short.com.
 * Chinese is the label; English is the locale fallback.
 * Login consent stays terms + privacy. Do not remove the filing number from LegalFooter.
 * Do not invent, shorten, or swap ICP suffixes. Do not invent slug paths or resurrect retired /legal/* paths.
 */


export const WEBSITE_ORIGIN = 'https://www.bgssai.com'

export const COPYRIGHT_HOLDER = '昆山兵贵神速智能科技有限公司'
export const COPYRIGHT_YEAR = new Date().getFullYear()
export const COPYRIGHT_LINE = `© ${COPYRIGHT_YEAR} ${COPYRIGHT_HOLDER}`

export const ICP_FILING_NUMBER = '苏ICP备2024070062号-15'
export const ICP_FILING_URL = 'https://beian.miit.gov.cn/'

/** Official pages. label_zh is the product label; label_en is the locale fallback. */
export const LEGAL_PAGES = Object.freeze([
  {
    key: 'terms',
    slug: 'terms-of-service',
    path_zh: '/terms-of-service/',
    path_en: '/en/terms-of-service/',
    label_zh: '用户协议',
    label_en: 'Terms of Service',
    footer_zh: '用户协议',
    footer_en: 'Terms of Service',
  },
  {
    key: 'privacy',
    slug: 'privacy-policy',
    path_zh: '/privacy-policy/',
    path_en: '/en/privacy-policy/',
    label_zh: '隐私政策',
    label_en: 'Privacy Policy',
    footer_zh: '隐私政策',
    footer_en: 'Privacy Policy',
  },
  {
    key: 'inventory',
    slug: 'personal-information-inventory',
    path_zh: '/personal-information-inventory/',
    path_en: '/en/personal-information-inventory/',
    label_zh: '个人信息收集清单',
    label_en: 'Personal Information Inventory',
    footer_zh: '个人信息收集清单',
    footer_en: 'Personal Information Inventory',
  },
  {
    key: 'sharing',
    slug: 'third-party-sharing',
    path_zh: '/third-party-sharing/',
    path_en: '/en/third-party-sharing/',
    label_zh: '第三方信息共享清单',
    label_en: 'Third-Party Sharing',
    footer_zh: '第三方信息共享清单',
    footer_en: 'Third-Party Sharing',
  },
  {
    key: 'permissions',
    slug: 'app-permissions',
    path_zh: '/app-permissions/',
    path_en: '/en/app-permissions/',
    label_zh: '应用权限说明',
    label_en: 'App Permissions',
    footer_zh: '应用权限说明',
    footer_en: 'App Permissions',
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

const FORBIDDEN_PATH_RE = /bgssai\.cn|\/legal\/user-agreement|children-privacy|personal-info-collection|dev\.bgssai\.com/i

export function legalPageUrl(page, locale = 'zh') {
  const path = locale === 'en' ? page.path_en : page.path_zh
  return `${WEBSITE_ORIGIN}${path}`
}

export function legalFooterLabel(page, locale = 'zh') {
  const zh = page?.label_zh || page?.label_en || ''
  const en = page?.label_en || page?.label_zh || ''
  return locale === 'en' ? en : zh
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
