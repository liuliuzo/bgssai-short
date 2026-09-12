/**
 * Official legal pages live only on bgssai-website.
 * This module is outbound links. Status: 需法务审阅.
 * Do not invent ICP / license numbers.
 */

export const LEGAL_REVIEW_STATUS = '需法务审阅'

export const WEBSITE_ORIGIN = 'https://www.bgssai.cn'

export const LEGAL_PAGES = Object.freeze([
  {
    slug: 'user-agreement',
    path: '/legal/user-agreement',
    label_zh: '用户协议',
    label_en: 'User Agreement',
  },
  {
    slug: 'privacy',
    path: '/legal/privacy',
    label_zh: '隐私政策',
    label_en: 'Privacy Policy',
  },
  {
    slug: 'children-privacy',
    path: '/legal/children-privacy',
    label_zh: '儿童个人信息保护规则',
    label_en: "Children's Privacy",
  },
  {
    slug: 'third-party-sharing',
    path: '/legal/third-party-sharing',
    label_zh: '第三方信息共享清单',
    label_en: 'Third-Party Sharing',
  },
  {
    slug: 'personal-info-collection',
    path: '/legal/personal-info-collection',
    label_zh: '个人信息收集清单',
    label_en: 'Personal Information Collection',
  },
])

const LICENSE_NUMBER_RE = /ICP|备案|增值电信|经营许可|京网安|沪公网|苏ICP|许可证号/i

export function legalPageUrl(page) {
  return `${WEBSITE_ORIGIN}${page.path}`
}

export function bilingualLabel(page) {
  return `${page.label_zh} / ${page.label_en}`
}

export function hasInventedLicenseNumber(text) {
  return LICENSE_NUMBER_RE.test(String(text || ''))
}
