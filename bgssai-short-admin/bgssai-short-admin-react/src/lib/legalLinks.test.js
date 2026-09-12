import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  LEGAL_PAGES,
  LEGAL_REVIEW_STATUS,
  WEBSITE_ORIGIN,
  bilingualLabel,
  hasInventedLicenseNumber,
  legalPageUrl,
} from './legalLinks.js'

describe('legalLinks', () => {
  it('exposes exactly five official website pages', () => {
    assert.equal(LEGAL_PAGES.length, 5)
    assert.equal(LEGAL_REVIEW_STATUS, '需法务审阅')
    assert.equal(WEBSITE_ORIGIN, 'https://www.bgssai.cn')
  })

  it('uses bilingual labels and outbound website URLs', () => {
    const slugs = new Set()
    for (const page of LEGAL_PAGES) {
      slugs.add(page.slug)
      assert.ok(page.label_zh)
      assert.ok(page.label_en)
      assert.match(page.label_zh, /[\u4e00-\u9fff]/)
      assert.match(page.label_en, /[A-Za-z]/)
      assert.equal(bilingualLabel(page), `${page.label_zh} / ${page.label_en}`)
      const url = legalPageUrl(page)
      assert.equal(url.startsWith(`${WEBSITE_ORIGIN}/legal/`), true)
      assert.equal(hasInventedLicenseNumber(url), false)
      assert.equal(hasInventedLicenseNumber(bilingualLabel(page)), false)
    }
    assert.equal(slugs.size, 5)
  })

  it('rejects invented license numbers in footer copy', () => {
    assert.equal(hasInventedLicenseNumber('© 2026 昆山兵贵神速智能科技有限公司'), false)
    assert.equal(hasInventedLicenseNumber('苏ICP备12345678号'), true)
    assert.equal(hasInventedLicenseNumber('增值电信业务经营许可证'), true)
  })
})
