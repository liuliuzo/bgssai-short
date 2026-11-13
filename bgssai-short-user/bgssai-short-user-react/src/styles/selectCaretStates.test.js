/**
 * Render-state checks for Select down-arrow override.
 * Uses Playwright + Ant Design UMD so we assert real computed styles / DOM,
 * not only CSS string presence.
 */
import assert from 'node:assert/strict'
import fs from 'node:fs'
import path from 'node:path'
import test from 'node:test'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { createRequire } from 'node:module'
import { chromium } from 'playwright-core'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const reactRoot = path.resolve(__dirname, '../..')
const caretCss = fs.readFileSync(
  path.join(reactRoot, 'src/styles/rounded-chevron-caret.css'),
  'utf8',
)

const require = createRequire(import.meta.url)
let chromePath = '/usr/local/bin/google-chrome'
try {
  // optional; fall back to system chrome
  require.resolve('playwright-core')
} catch {
  /* ignore */
}

function fixtureHtml() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8" />
<meta name="viewport" content="width=device-width, initial-scale=1" />
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/antd@5.29.3/dist/reset.css" />
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/antd@5.29.3/dist/antd.min.css" />
<style>${caretCss}</style>
</head>
<body style="padding:24px;background:#fff;font-family:sans-serif">
<div id="root"></div>
<script src="https://cdn.jsdelivr.net/npm/react@18.3.1/umd/react.production.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/react-dom@18.3.1/umd/react-dom.production.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/dayjs@1.11.13/dayjs.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/antd@5.29.3/dist/antd.min.js"></script>
<script>
const { useState } = React;
const { Select, Space, Collapse } = antd;
const opts = [
  { value: 'a', label: '复仇逆袭' },
  { value: 'b', label: '都市写实' },
];
function App() {
  const [open, setOpen] = useState(false);
  return React.createElement(Space, { direction: 'vertical', size: 16, style: { width: 360 } },
    React.createElement('div', { 'data-case': 'basic' },
      React.createElement(Select, {
        id: 'sel-basic',
        defaultValue: 'a',
        options: opts,
        style: { width: '100%' },
        open,
        onDropdownVisibleChange: setOpen,
        'aria-label': '题材',
      })
    ),
    React.createElement('div', { 'data-case': 'search' },
      React.createElement(Select, {
        id: 'sel-search',
        showSearch: true,
        defaultValue: 'a',
        options: opts,
        style: { width: '100%' },
        optionFilterProp: 'label',
        'aria-label': '画风搜索',
      })
    ),
    React.createElement('div', { 'data-case': 'loading' },
      React.createElement(Select, {
        id: 'sel-loading',
        loading: true,
        defaultValue: 'a',
        options: opts,
        style: { width: '100%' },
        'aria-label': '加载中',
      })
    ),
    React.createElement('div', { 'data-case': 'clear' },
      React.createElement(Select, {
        id: 'sel-clear',
        allowClear: true,
        defaultValue: 'a',
        options: opts,
        style: { width: '100%' },
        'aria-label': '可清除',
      })
    ),
    React.createElement('div', { 'data-case': 'disabled' },
      React.createElement(Select, {
        id: 'sel-disabled',
        disabled: true,
        defaultValue: 'a',
        options: opts,
        style: { width: '100%' },
        'aria-label': '禁用',
      })
    ),
    React.createElement('div', { 'data-case': 'custom-suffix' },
      React.createElement(Select, {
        id: 'sel-custom-suffix',
        defaultValue: 'a',
        options: opts,
        style: { width: '100%' },
        suffixIcon: React.createElement('span', {
          className: 'anticon anticon-search custom-suffix-mark',
          'aria-label': 'search',
          role: 'img',
        }, 'Q'),
        'aria-label': '自定义后缀',
      })
    ),
    React.createElement('div', { 'data-case': 'collapse' },
      React.createElement(Collapse, {
        items: [{ key: '1', label: '折叠面板', children: 'content' }],
      })
    ),
  );
}
ReactDOM.createRoot(document.getElementById('root')).render(React.createElement(App));
</script>
</body>
</html>`
}

async function withPage(fn) {
  const browser = await chromium.launch({
    executablePath: chromePath,
    args: ['--no-sandbox', '--disable-dev-shm-usage'],
  })
  try {
    const page = await browser.newPage({ viewport: { width: 720, height: 900 } })
    await page.setContent(fixtureHtml(), { waitUntil: 'networkidle' })
    await page.waitForSelector('.ant-select-arrow', { timeout: 15000 })
    return await fn(page)
  } finally {
    await browser.close()
  }
}

test('basic select: down svg hidden; rounded pseudo chevron present', async () => {
  await withPage(async (page) => {
    const info = await page.evaluate(() => {
      const root = document.querySelector('[data-case="basic"] .ant-select-arrow')
      const down = root && root.querySelector(':scope > .anticon-down')
      const svg = down && down.querySelector('svg[data-icon="down"]')
      const before = getComputedStyle(root, '::before')
      const after = getComputedStyle(root, '::after')
      return {
        hasDown: !!down,
        downDisplay: down ? getComputedStyle(down).display : null,
        svgExists: !!svg,
        beforeContent: before.content,
        beforeRadius: before.borderRadius,
        beforeH: before.height,
        afterContent: after.content,
        arrowW: getComputedStyle(root).width,
        arrowH: getComputedStyle(root).height,
      }
    })
    assert.equal(info.hasDown, true)
    assert.equal(info.downDisplay, 'none')
    assert.equal(info.svgExists, true)
    assert.notEqual(info.beforeContent, 'none')
    assert.equal(info.beforeRadius, '999px')
    assert.equal(info.beforeH, '1.75px')
    assert.notEqual(info.afterContent, 'none')
    await page.locator('[data-case="basic"] .ant-select').screenshot({
      path: '/opt/cursor/artifacts/select-caret-basic.png',
    })
  })
})

test('open select rotates arrow node (chevron follows)', async () => {
  await withPage(async (page) => {
    await page.click('[data-case="basic"] .ant-select-selector')
    await page.waitForSelector('[data-case="basic"] .ant-select-open', { timeout: 5000 })
    // Wait past the 0.15s transform transition so we assert the settled 180deg matrix.
    await page.waitForFunction(() => {
      const arrow = document.querySelector('[data-case="basic"] .ant-select-arrow')
      const t = getComputedStyle(arrow).transform
      return /matrix\(-1[,\s]/.test(t)
    }, { timeout: 3000 })
    const rot = await page.evaluate(() => {
      const arrow = document.querySelector('[data-case="basic"] .ant-select-arrow')
      return getComputedStyle(arrow).transform
    })
    assert.match(rot, /matrix\(-1/)
    await page.locator('[data-case="basic"] .ant-select').screenshot({
      path: '/opt/cursor/artifacts/select-caret-open.png',
    })
    await page.keyboard.press('Escape')
    await page.waitForSelector('[data-case="basic"] .ant-select-open', {
      state: 'detached',
      timeout: 5000,
    })
    await page.waitForFunction(() => {
      const arrow = document.querySelector('[data-case="basic"] .ant-select-arrow')
      return getComputedStyle(arrow).transform === 'none'
    }, { timeout: 3000 })
  })
})

test('loading select: keeps loading icon; no fake chevron pseudos', async () => {
  await withPage(async (page) => {
    const info = await page.evaluate(() => {
      const root = document.querySelector('[data-case="loading"] .ant-select-arrow')
      const down = root && root.querySelector(':scope > .anticon-down')
      const loading = root && root.querySelector('.anticon-loading, .ant-select-arrow-loading, .anticon-spin')
      const spin = root && root.querySelector('[aria-label="loading"], .anticon-loading')
      const anyIcon = root && root.querySelector('.anticon')
      const before = getComputedStyle(root, '::before')
      return {
        hasDown: !!down,
        hasLoading: !!(loading || spin),
        iconDisplay: anyIcon ? getComputedStyle(anyIcon).display : null,
        beforeContent: before.content,
        arrowHtml: root ? root.innerHTML.slice(0, 200) : null,
      }
    })
    assert.equal(info.hasDown, false, 'loading should not keep anticon-down')
    assert.equal(info.beforeContent, 'none', 'pseudo chevron must not show without down')
    assert.notEqual(info.iconDisplay, 'none')
    await page.locator('[data-case="loading"] .ant-select').screenshot({
      path: '/opt/cursor/artifacts/select-caret-loading.png',
    })
  })
})

test('clear control stays usable; down still replaced', async () => {
  await withPage(async (page) => {
    const box = page.locator('[data-case="clear"] .ant-select')
    await box.hover()
    await page.waitForSelector('[data-case="clear"] .ant-select-clear', { timeout: 5000 })
    const clearVisible = await page.locator('[data-case="clear"] .ant-select-clear').isVisible()
    assert.equal(clearVisible, true)
    const downHidden = await page.evaluate(() => {
      const down = document.querySelector('[data-case="clear"] .ant-select-arrow > .anticon-down')
      return down ? getComputedStyle(down).display : null
    })
    assert.equal(downHidden, 'none')
    await box.screenshot({ path: '/opt/cursor/artifacts/select-caret-clear.png' })
  })
})

test('disabled select keeps chevron but does not block pointer on selector', async () => {
  await withPage(async (page) => {
    const info = await page.evaluate(() => {
      const sel = document.querySelector('[data-case="disabled"] .ant-select')
      const arrow = sel.querySelector('.ant-select-arrow')
      const down = arrow.querySelector(':scope > .anticon-down')
      const before = getComputedStyle(arrow, '::before')
      return {
        disabled: sel.classList.contains('ant-select-disabled'),
        downDisplay: getComputedStyle(down).display,
        beforeContent: before.content,
        opacity: getComputedStyle(arrow).opacity,
      }
    })
    assert.equal(info.disabled, true)
    assert.equal(info.downDisplay, 'none')
    assert.notEqual(info.beforeContent, 'none')
    assert.ok(Number(info.opacity) < 0.5)
  })
})

test('custom suffix stays visible; no fake chevron when no anticon-down', async () => {
  await withPage(async (page) => {
    const info = await page.evaluate(() => {
      const root = document.querySelector('[data-case="custom-suffix"] .ant-select-arrow')
      const down = root && root.querySelector(':scope > .anticon-down')
      const custom = root && root.querySelector('.custom-suffix-mark')
      const before = getComputedStyle(root, '::before')
      return {
        hasDown: !!down,
        customDisplay: custom ? getComputedStyle(custom).display : null,
        beforeContent: before.content,
        customText: custom ? custom.textContent : null,
      }
    })
    assert.equal(info.hasDown, false)
    assert.equal(info.beforeContent, 'none')
    assert.notEqual(info.customDisplay, 'none')
    assert.equal(info.customText, 'Q')
    await page.locator('[data-case="custom-suffix"] .ant-select').screenshot({
      path: '/opt/cursor/artifacts/select-caret-custom-suffix.png',
    })
  })
})

test('collapse right arrow is not hidden by select caret rules', async () => {
  await withPage(async (page) => {
    const info = await page.evaluate(() => {
      const right = document.querySelector('.ant-collapse .anticon-right, .ant-collapse-arrow .anticon')
      if (!right) return { found: false }
      return {
        found: true,
        display: getComputedStyle(right).display,
        visibility: getComputedStyle(right).visibility,
      }
    })
    assert.equal(info.found, true)
    assert.notEqual(info.display, 'none')
    assert.notEqual(info.visibility, 'hidden')
  })
})

test('keyboard: ArrowDown opens listbox on focused select', async () => {
  await withPage(async (page) => {
    const input = page.locator('[data-case="basic"] .ant-select-selection-search-input')
    await input.focus()
    await input.press('ArrowDown')
    await page.waitForSelector('[data-case="basic"] .ant-select-open', { timeout: 5000 })
    await page.waitForSelector('.ant-select-dropdown:not(.ant-select-dropdown-hidden)', {
      timeout: 5000,
    })
    const open = await page.locator('[data-case="basic"] .ant-select-open').count()
    assert.ok(open > 0)
    await page.keyboard.press('Escape')
    await page.waitForSelector('[data-case="basic"] .ant-select-open', {
      state: 'detached',
      timeout: 5000,
    })
  })
})

test('admin caret CSS stays in lockstep with user caret CSS', async () => {
  const adminCss = fs.readFileSync(
    path.resolve(
      reactRoot,
      '../../bgssai-short-admin/bgssai-short-admin-react/src/styles/rounded-chevron-caret.css',
    ),
    'utf8',
  )
  assert.equal(adminCss, caretCss)
})
