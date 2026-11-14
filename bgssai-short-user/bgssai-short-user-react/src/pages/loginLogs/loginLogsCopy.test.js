import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))

test('/login-logs navigation and heading use 登录日志 instead of 我的登录日志', () => {
  const layoutSrc = readFileSync(resolve(here, '../../layouts/MainLayout.jsx'), 'utf8')
  const pageSrc = readFileSync(resolve(here, './LoginLogsPage.jsx'), 'utf8')

  assert.equal(layoutSrc.includes('我的登录日志'), false)
  assert.equal(pageSrc.includes('我的登录日志'), false)
  assert.match(layoutSrc, /key:\s*'\/login-logs',\s*label:\s*'登录日志'/)
  assert.match(pageSrc, />登录日志<\/Title>/)
  assert.match(pageSrc, /方式/)
  assert.match(pageSrc, /设备/)
  assert.match(pageSrc, /未知/)
})
