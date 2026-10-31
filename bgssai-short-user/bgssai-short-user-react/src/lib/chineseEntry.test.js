import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { CHINESE_ENTRY_PATH, englishPrefixTarget, isEnglishPrefix } from './chineseEntry.js'

describe('english prefix alias', () => {
  it('sends /en and /en/* to the Chinese login', () => {
    assert.equal(CHINESE_ENTRY_PATH, '/login')
    for (const path of ['/en', '/en/', '/en/login', '/en/home', '/en/privacy-policy', '/en/works/1/script']) {
      assert.equal(isEnglishPrefix(path), true)
      assert.equal(englishPrefixTarget(path), '/login')
    }
  })

  it('keeps query and hash on the Chinese login', () => {
    assert.equal(englishPrefixTarget('/en/login', '?redirect=%2Fhome', '#panel'), '/login?redirect=%2Fhome#panel')
  })

  it('leaves real paths, including lookalikes and unknown pages, alone', () => {
    for (const path of ['/', '/login', '/home', '/enterprise', '/end', '/english', '/no-such-page', '/privacy', '/privacy-policy']) {
      assert.equal(isEnglishPrefix(path), false)
      assert.equal(englishPrefixTarget(path), null)
    }
  })
})
