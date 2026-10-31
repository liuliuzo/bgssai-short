import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { CHINESE_ENTRY_PATH, englishPrefixTarget, isEnglishPrefix } from './chineseEntry.js'

describe('english prefix alias', () => {
  it('sends /en and /en/* to the Chinese login', () => {
    assert.equal(CHINESE_ENTRY_PATH, '/login')
    for (const path of ['/en', '/en/', '/en/login', '/en/home', '/en/privacy-policy']) {
      assert.equal(isEnglishPrefix(path), true)
      assert.equal(englishPrefixTarget(path), '/login')
    }
  })

  it('keeps query and hash on the Chinese login', () => {
    assert.equal(englishPrefixTarget('/en', '?from=en', ''), '/login?from=en')
  })

  it('leaves real paths, including lookalikes and unknown pages, alone', () => {
    for (const path of ['/', '/login', '/home', '/enterprise', '/end', '/no-such-admin-page', '/privacy-policy']) {
      assert.equal(isEnglishPrefix(path), false)
      assert.equal(englishPrefixTarget(path), null)
    }
  })
})
