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
  legalPageUrl,
  COPYRIGHT_LINE,
  LOGIN_CONSENT_PAGES,
} from './legalLinks.js'

const here = dirname(fileURLToPath(import.meta.url))

const REQUIRED_SLUGS = [
  'terms-of-service',
  'privacy-policy',
]

const FORBIDDEN_SLUGS = [
  'personal-information-inventory',
  'third-party-sharing',
  'app-permissions',
]

const FORBIDDEN_LABELS = [
  '个人信息收集清单',
  '第三方信息共享清单',
  '应用权限',
  'Personal Information Inventory',
  'Third-Party Sharing',
  'App Permissions',
]

describe('legalLinks', () => {
  it('exposes terms + privacy only on www.bgssai.com (Standards §15)', () => {
    assert.equal(LEGAL_PAGES.length, 2)
    assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.com')
    assert.deepEqual(LEGAL_PAGES.map((page) => page.slug), REQUIRED_SLUGS)
    const blob = JSON.stringify(LEGAL_PAGES)
    for (const slug of FORBIDDEN_SLUGS) {
      assert.equal(blob.includes(slug), false, `LEGAL_PAGES must not include ${slug}`)
    }
    for (const label of FORBIDDEN_LABELS) {
      assert.equal(blob.includes(label), false, `LEGAL_PAGES must not include ${label}`)
    }
  })

  it('uses bilingual labels and exact zh / en website paths', () => {
    for (const page of LEGAL_PAGES) {
      assert.ok(page.label_zh)
      assert.ok(page.label_en)
      assert.match(page.label_zh, /[\u4e00-\u9fff]/)
      assert.match(page.label_en, /[A-Za-z]/)
      assert.equal(bilingualLabel(page), `${page.label_zh} / ${page.label_en}`)
      assert.ok(page.footer_zh)
      assert.ok(page.footer_en)
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

  it('forbids inventory slugs/labels in legalLinks.js source', () => {
    const src = readFileSync(resolve(here, 'legalLinks.js'), 'utf8')
    for (const slug of FORBIDDEN_SLUGS) {
      assert.equal(src.includes(slug), false, `legalLinks.js must not include ${slug}`)
    }
    for (const label of FORBIDDEN_LABELS) {
      assert.equal(src.includes(label), false, `legalLinks.js must not include ${label}`)
    }
  })
})
