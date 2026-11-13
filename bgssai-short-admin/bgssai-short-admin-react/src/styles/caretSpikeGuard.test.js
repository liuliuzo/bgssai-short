/**
 * Guard: admin shipped CSS must not reintroduce CSS border-triangle carets.
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

test('admin react source CSS has no border-triangle carets', () => {
  const files = listCssFiles(path.join(reactRoot, 'src'))
  assert.ok(files.length > 0, 'expected CSS under admin react src')
  assert.deepEqual(findSpikes(files), [])
})

test('admin shipped static CSS has no border-triangle carets', () => {
  const staticDir = path.join(
    repoRoot,
    'bgssai-short-admin/bgssai-short-admin/src/main/resources/static/assets',
  )
  const files = listCssFiles(staticDir)
  if (files.length === 0) return
  assert.deepEqual(findSpikes(files), [])
})

test('admin ant select caret override is wired', () => {
  const css = fs.readFileSync(
    path.join(reactRoot, 'src/styles/rounded-chevron-caret.css'),
    'utf8',
  )
  assert.match(css, /\.ant-select\s+\.ant-select-arrow::before/)
  assert.match(css, /border-radius:\s*999px/)
  const main = fs.readFileSync(path.join(reactRoot, 'src/main.jsx'), 'utf8')
  assert.match(main, /rounded-chevron-caret\.css/)
})
