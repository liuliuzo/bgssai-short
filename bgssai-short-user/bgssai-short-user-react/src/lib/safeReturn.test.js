import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import { safeReturnPath } from './safeReturn.js'

describe('safeReturnPath', () => {
  it('keeps in-app paths and drops open redirects', () => {
    assert.equal(safeReturnPath('/works/w-1?tab=script'), '/works/w-1?tab=script')
    assert.equal(safeReturnPath('https://evil.example/works'), null)
    assert.equal(safeReturnPath('//evil.example'), null)
    assert.equal(safeReturnPath('/login'), null)
    assert.equal(safeReturnPath('/\\evil'), null)
  })
})
