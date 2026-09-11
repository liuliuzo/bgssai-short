import { describe, it } from 'node:test'
import assert from 'node:assert/strict'
import {
  isTruthyFlag,
  isFalsyFlag,
  readFlagsFromSearch,
  readFlagsFromLocation,
  resolveShellMode,
  applyBgssaiShell,
  SHELL_STORAGE_KEY,
} from './bgssaiShell.js'

describe('bgssaiShell flags', () => {
  it('accepts 1 / true / yes', () => {
    assert.equal(isTruthyFlag('1'), true)
    assert.equal(isTruthyFlag('TRUE'), true)
    assert.equal(isTruthyFlag('yes'), true)
    assert.equal(isTruthyFlag('2'), false)
  })

  it('accepts 0 / false / no as explicit off', () => {
    assert.equal(isFalsyFlag('0'), true)
    assert.equal(isFalsyFlag('False'), true)
    assert.equal(isFalsyFlag('no'), true)
  })

  it('detects bgssai_shell and chat_pane in search', () => {
    assert.equal(readFlagsFromSearch('?bgssai_shell=1'), true)
    assert.equal(readFlagsFromSearch('chat_pane=true&foo=bar'), true)
    assert.equal(readFlagsFromSearch('?bgssai_shell=0'), false)
    assert.equal(readFlagsFromSearch('?foo=1'), null)
  })

  it('prefers search over hash and reads hash when search is empty', () => {
    assert.equal(readFlagsFromLocation({ search: '?bgssai_shell=1', hash: '#/home?chat_pane=0' }), true)
    assert.equal(readFlagsFromLocation({ search: '', hash: '#/login?chat_pane=1' }), true)
    assert.equal(readFlagsFromLocation({ search: '', hash: '#/home' }), null)
  })

  it('persists across SPA hops and honors explicit off', () => {
    const storage = new Map()
    const store = {
      getItem: (k) => (storage.has(k) ? storage.get(k) : null),
      setItem: (k, v) => storage.set(k, String(v)),
      removeItem: (k) => storage.delete(k),
    }

    const on = resolveShellMode({ search: '?bgssai_shell=1', hash: '' }, store)
    assert.equal(on.active, true)
    applyBgssaiShell(true, { documentElement: { setAttribute() {}, removeAttribute() {} }, body: { classList: { toggle() {} } } }, store)
    assert.equal(store.getItem(SHELL_STORAGE_KEY), '1')

    const hop = resolveShellMode({ search: '', hash: '' }, store)
    assert.equal(hop.active, true)

    const off = resolveShellMode({ search: '?chat_pane=0', hash: '' }, store)
    assert.equal(off.active, false)
  })

  it('does not treat viewport-less standalone as shell', () => {
    const hop = resolveShellMode({ search: '', hash: '' }, { getItem: () => null })
    assert.equal(hop.active, false)
  })
})
