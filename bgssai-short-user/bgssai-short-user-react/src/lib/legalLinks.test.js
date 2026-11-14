import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  LEGAL_HUB,
  LEGAL_PAGES,
  WEBSITE_ORIGIN,
  bilingualLabel,
  hasForbiddenLegalPath,
  hasInventedLicenseNumber,
  legalFooterLabel,
  legalPageUrl,
  COPYRIGHT_LINE,
  ICP_FILING_NUMBER,
  LOGIN_CONSENT_PAGES,
} from './legalLinks.js'

const here = dirname(fileURLToPath(import.meta.url))

const REQUIRED_SLUGS = [
  'terms-of-service',
  'privacy-policy',
  'personal-information-inventory',
  'third-party-sharing',
  'app-permissions',
]

describe('legalLinks', () => {
  it('exposes five official pages on www.bgssai.com', () => {
    assert.equal(LEGAL_PAGES.length, 5)
    assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.com')
    assert.deepEqual(LEGAL_PAGES.map((page) => page.slug), REQUIRED_SLUGS)
    const blob = JSON.stringify(LEGAL_PAGES)
    assert.equal(blob.includes('dev.bgssai.com'), false)
    assert.equal(blob.includes('/legal/terms'), false)
    assert.equal(blob.includes('/legal/privacy'), false)
  })

  it('uses Chinese labels with English locale fallbacks and exact website paths', () => {
    for (const page of LEGAL_PAGES) {
      assert.ok(page.label_zh)
      assert.ok(page.label_en)
      assert.match(page.label_zh, /[\u4e00-\u9fff]/)
      assert.match(page.label_en, /[A-Za-z]/)
      assert.equal(legalFooterLabel(page, 'zh'), page.label_zh)
      assert.equal(legalFooterLabel(page, 'en'), page.label_en)
      assert.equal(bilingualLabel(page), `${page.label_zh} / ${page.label_en}`)
      assert.equal(page.path_zh, `/${page.slug}/`)
      assert.equal(page.path_en, `/en/${page.slug}/`)
      const zhUrl = legalPageUrl(page, 'zh')
      const enUrl = legalPageUrl(page, 'en')
      assert.equal(zhUrl, `${WEBSITE_ORIGIN}/${page.slug}/`)
      assert.equal(enUrl, `${WEBSITE_ORIGIN}/en/${page.slug}/`)
      assert.equal(hasInventedLicenseNumber(zhUrl), false)
      assert.equal(hasForbiddenLegalPath(zhUrl), false)
      assert.equal(hasForbiddenLegalPath(enUrl), false)
    }
  })

  it('exposes optional legal hub paths without inventing licenses', () => {
    assert.equal(legalPageUrl(LEGAL_HUB, 'zh'), 'https://www.bgssai.com/legal/')
    assert.equal(legalPageUrl(LEGAL_HUB, 'en'), 'https://www.bgssai.com/en/legal/')
    assert.equal(hasInventedLicenseNumber(bilingualLabel(LEGAL_HUB)), false)
  })

  it('login consent is terms + privacy only', () => {
    assert.deepEqual(LOGIN_CONSENT_PAGES.map((p) => p.slug), ['terms-of-service', 'privacy-policy'])
    assert.equal(COPYRIGHT_LINE, `© ${new Date().getFullYear()} 昆山兵贵神速智能科技有限公司`)
    assert.equal(hasInventedLicenseNumber(COPYRIGHT_LINE), false)
    assert.equal(hasInventedLicenseNumber(ICP_FILING_NUMBER), false)
  })

  it('renders five Chinese footer links and does not render an ICP filing', () => {
    const footerSrc = readFileSync(resolve(here, '../components/LegalFooter.jsx'), 'utf8')
    assert.equal(footerSrc.includes('LEGAL_PAGES'), true)
    assert.equal(footerSrc.includes('legalFooterLabel'), true)
    assert.equal(footerSrc.includes('ICP_FILING_NUMBER'), false)
    assert.equal(footerSrc.includes('ICP_FILING_URL'), false)
    assert.equal(footerSrc.includes('beian.miit.gov.cn'), false)
    assert.equal(footerSrc.includes('target="_blank"'), true)
    assert.equal(footerSrc.includes('rel="noopener noreferrer"'), true)
    assert.equal(footerSrc.includes('legal-footer-line'), true)
    assert.equal(footerSrc.includes('LOGIN_CONSENT_PAGES'), true)
    const css = readFileSync(resolve(here, '../components/legal-footer.css'), 'utf8')
    const hover = css.match(/\.legal-footer a:hover\s*\{[^}]*\}/)
    assert.ok(hover)
    assert.equal(hover[0].includes('text-decoration: none'), true)
    assert.equal(css.includes('background: transparent'), true)
  })

  it('rejects invented license numbers and retired legal paths', () => {
    assert.equal(hasInventedLicenseNumber('© 2026 昆山兵贵神速智能科技有限公司'), false)
    assert.equal(hasInventedLicenseNumber('苏ICP备2024070062号-15'), false)
    assert.equal(hasInventedLicenseNumber('苏ICP备12345678号'), true)
    assert.equal(hasInventedLicenseNumber('增值电信业务经营许可证'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.cn/legal/user-agreement'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/legal/children-privacy'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/legal/personal-info-collection'), true)
    assert.equal(hasForbiddenLegalPath('https://dev.bgssai.com/terms-of-service/'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/terms-of-service/'), false)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/app-permissions/'), false)
  })
})
