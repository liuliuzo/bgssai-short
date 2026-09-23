import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  LEGAL_HUB,
  LEGAL_PAGES,
  WEBSITE_ORIGIN,
  bilingualLabel,
  hasForbiddenLegalPath,
  hasInventedLicenseNumber,
  legalPageUrl,
  COPYRIGHT_LINE,
  LOGIN_CONSENT_PAGES,
} from './legalLinks.js'

const REQUIRED_SLUGS = [
  'terms-of-service',
  'privacy-policy',
]

describe('legalLinks', () => {
  it('exposes exactly two official website pages on www.bgssai.com', () => {
    assert.equal(LEGAL_PAGES.length, 2)
    assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.com')
    assert.deepEqual(LEGAL_PAGES.map((page) => page.slug), REQUIRED_SLUGS)
  })

  it('uses bilingual labels and exact zh / en website paths', () => {
    for (const page of LEGAL_PAGES) {
      assert.ok(page.label_zh)
      assert.ok(page.label_en)
      assert.match(page.label_zh, /[\u4e00-\u9fff]/)
      assert.match(page.label_en, /[A-Za-z]/)
      assert.equal(bilingualLabel(page), `${page.label_zh} / ${page.label_en}`)
      assert.equal(page.path_zh, `/${page.slug}/`)
      assert.equal(page.path_en, `/en/${page.slug}/`)
      const zhUrl = legalPageUrl(page, 'zh')
      const enUrl = legalPageUrl(page, 'en')
      assert.equal(zhUrl, `${WEBSITE_ORIGIN}/${page.slug}/`)
      assert.equal(enUrl, `${WEBSITE_ORIGIN}/en/${page.slug}/`)
      assert.equal(hasInventedLicenseNumber(zhUrl), false)
      assert.equal(hasInventedLicenseNumber(enUrl), false)
      assert.equal(hasInventedLicenseNumber(bilingualLabel(page)), false)
      assert.equal(hasForbiddenLegalPath(zhUrl), false)
      assert.equal(hasForbiddenLegalPath(enUrl), false)
    }
  })

  it('exposes optional legal hub paths without inventing licenses', () => {
    assert.equal(legalPageUrl(LEGAL_HUB, 'zh'), 'https://www.bgssai.com/legal/')
    assert.equal(legalPageUrl(LEGAL_HUB, 'en'), 'https://www.bgssai.com/en/legal/')
    assert.equal(hasInventedLicenseNumber(bilingualLabel(LEGAL_HUB)), false)
  })


  it('login consent is terms + privacy only; copyright has no ICP', () => {
    assert.deepEqual(LOGIN_CONSENT_PAGES.map((p) => p.slug), ['terms-of-service', 'privacy-policy'])
    assert.equal(COPYRIGHT_LINE, `© ${new Date().getFullYear()} 昆山兵贵神速智能科技有限公司`)
    assert.equal(hasInventedLicenseNumber(COPYRIGHT_LINE), false)
  })

  it('rejects invented license numbers and retired legal paths', () => {
    assert.equal(hasInventedLicenseNumber('© 2026 昆山兵贵神速智能科技有限公司'), false)
    assert.equal(hasInventedLicenseNumber('苏ICP备12345678号'), true)
    assert.equal(hasInventedLicenseNumber('增值电信业务经营许可证'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.cn/legal/user-agreement'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/legal/children-privacy'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/legal/personal-info-collection'), true)
    assert.equal(hasForbiddenLegalPath('https://www.bgssai.com/terms-of-service/'), false)
  })
})
