/**
 * Guard: shipped Short CSS must not reintroduce CSS border-triangle carets,
 * and Select override must target only anticon-down (not all anticon).
 */
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath } from 'node:url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const reactRoot = path.resolve(__dirname, '../..')
const repoRoot = path.resolve(reactRoot, '../..')

const SPIKE_RE =
  /width\s*:\s*0\s*;[^}]*height\s*:\s*0\s*;[^}]*border-(?:left|right)\s*:\s*\d+px\s+solid\s+transparent[^}]*border-top\s*:\s*\d+px\s+solid/i

function listCssFiles(dir, acc = []) {
  if (!fs.existsSync(dir)) return acc
  for (const entry of fs.readdirSync(dir, { withFileTypes: true })) {
    if (entry.name === 'node_modules' || entry.name === '.git') continue
    const full = path.join(dir, entry.name)
    if (entry.isDirectory()) listCssFiles(full, acc)
    else if (/\.css$/i.test(entry.name)) acc.push(full)
  }
  return acc
}

function findSpikes(files) {
  const hits = []
  for (const file of files) {
    const text = fs.readFileSync(file, 'utf8')
    if (SPIKE_RE.test(text)) hits.push(path.relative(repoRoot, file))
  }
  return hits
}

test('user react source CSS has no border-triangle carets', () => {
  const files = listCssFiles(path.join(reactRoot, 'src'))
  assert.ok(files.length > 0)
  assert.deepEqual(findSpikes(files), [])
})

test('user shipped static CSS has no border-triangle carets', () => {
  const staticDir = path.join(
    repoRoot,
    'bgssai-short-user/bgssai-short-user/src/main/resources/static/assets',
  )
  const files = listCssFiles(staticDir)
  if (files.length === 0) return
  assert.deepEqual(findSpikes(files), [])
})

test('login phone-dial caret uses rounded stroke segments', () => {
  const css = fs.readFileSync(
    path.join(reactRoot, 'src/components/phone-dial-search.css'),
    'utf8',
  )
  assert.match(css, /\.phone-dial-combo-caret::before/)
  assert.match(css, /border-radius:\s*999px/)
  assert.doesNotMatch(css, /border-top:\s*5px\s+solid\s+currentColor/)
})

test('select caret override only hides anticon-down and gates pseudos with :has', () => {
  const css = fs.readFileSync(
    path.join(reactRoot, 'src/styles/rounded-chevron-caret.css'),
    'utf8',
  )
  assert.match(css, /\.ant-select-arrow:has\(\s*>\s*\.anticon-down\s*\)/)
  assert.match(css, /\.ant-select-arrow\s*>\s*\.anticon-down/)
  assert.doesNotMatch(
    css,
    /\.ant-select-arrow\s+\.anticon\s*\{[^}]*display\s*:\s*none/s,
    'must not hide all .anticon under .ant-select-arrow',
  )
  assert.match(css, /:has\(\s*>\s*\.anticon-down\s*\)::before/)
  assert.match(css, /\.ant-select-open\s+\.ant-select-arrow:has\(\s*>\s*\.anticon-down\s*\)/)
  const main = fs.readFileSync(path.join(reactRoot, 'src/main.jsx'), 'utf8')
  assert.match(main, /rounded-chevron-caret\.css/)
  const rebuilt = listCssFiles(
    path.join(repoRoot, 'bgssai-short-user/bgssai-short-user/src/main/resources/static/assets'),
  )
  assert.ok(rebuilt.some((f) => {
    const text = fs.readFileSync(f, 'utf8')
    return (
      text.includes(':has(>.anticon-down)') ||
      text.includes(':has(> .anticon-down)') ||
      /:has\(>\.anticon-down\)/.test(text.replace(/\s+/g, ''))
    )
  }), 'rebuilt static must include narrowed :has(> .anticon-down) caret')
})
