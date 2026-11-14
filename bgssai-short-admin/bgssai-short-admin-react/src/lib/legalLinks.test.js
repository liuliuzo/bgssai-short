import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  COPYRIGHT_LINE,
  ICP_FILING_NUMBER,
  LEGAL_HUB,
  LEGAL_PAGES,
  WEBSITE_ORIGIN,
  bilingualLabel,
  hasForbiddenLegalPath,
  hasInventedLicenseNumber,
  legalFooterLabel,
  legalPageUrl,
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
  })

  it('uses Chinese labels with English locale fallbacks and exact website paths', () => {
    for (const page of LEGAL_PAGES) {
      assert.equal(legalFooterLabel(page, 'zh'), page.label_zh)
      assert.equal(legalFooterLabel(page, 'en'), page.label_en)
      assert.equal(bilingualLabel(page), `${page.label_zh} / ${page.label_en}`)
      assert.equal(page.path_zh, `/${page.slug}/`)
      assert.equal(page.path_en, `/en/${page.slug}/`)
      assert.equal(legalPageUrl(page, 'zh'), `${WEBSITE_ORIGIN}/${page.slug}/`)
      assert.equal(legalPageUrl(page, 'en'), `${WEBSITE_ORIGIN}/en/${page.slug}/`)
      assert.equal(hasForbiddenLegalPath(legalPageUrl(page, 'zh')), false)
    }
  })

  it('exposes optional legal hub paths without inventing licenses', () => {
    assert.equal(legalPageUrl(LEGAL_HUB, 'zh'), 'https://www.bgssai.com/legal/')
    assert.equal(legalPageUrl(LEGAL_HUB, 'en'), 'https://www.bgssai.com/en/legal/')
  })

  it('defines copyright without putting the filing number in the footer', () => {
    assert.equal(COPYRIGHT_LINE, `© ${new Date().getFullYear()} 昆山兵贵神速智能科技有限公司`)
    assert.equal(hasInventedLicenseNumber(COPYRIGHT_LINE), false)
    assert.equal(hasInventedLicenseNumber(ICP_FILING_NUMBER), false)
    const footerSrc = readFileSync(resolve(here, '../components/LegalFooter.jsx'), 'utf8')
    assert.equal(footerSrc.includes('ICP_FILING_NUMBER'), false)
    assert.equal(footerSrc.includes('beian.miit.gov.cn'), false)
    assert.equal(footerSrc.includes('legalFooterLabel(page, \'zh\')'), true)
    assert.equal(footerSrc.includes('legalFooterLabel(page, \'en\')'), true)
    assert.equal(footerSrc.includes('target="_blank"'), true)
    assert.equal(footerSrc.includes('rel="noopener noreferrer"'), true)
  })

  it('rejects invented license numbers and retired legal paths', () => {
    assert.equal(hasInventedLicenseNumber('苏ICP备12345678号'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.cn/legal/user-agreement'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/legal/personal-info-collection'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/terms-of-service/'), false)
  })
})
