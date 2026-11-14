import { test, expect } from '@playwright/test'

const userPassword = process.env.SHORT_USER_PASSWORD
const adminPassword = process.env.SHORT_ADMIN_PASSWORD
const userName = process.env.SHORT_USER
const adminName = process.env.SHORT_ADMIN

async function loginUser(page) {
  await page.goto('/login')
  await page.getByRole('tab', { name: '密码' }).click()
  await page.getByPlaceholder('手机号 / 邮箱 / 账号').fill(userName)
  await page.getByPlaceholder('请输入密码').fill(userPassword)
  const agree = page.locator('input[type="checkbox"]')
  if (await agree.count()) await agree.first().check()
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/home/)
}

async function loginAdmin(page) {
  await page.goto('/login')
  await page.locator('#admin-username').fill(adminName)
  await page.locator('#admin-password').fill(adminPassword)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/home/)
}

test('user pages render without internal copy', async ({ page, baseURL }) => {
  test.skip(!userPassword || !userName, 'SHORT_USER credentials are not set')
  test.skip(!(baseURL?.includes(':8082') || baseURL?.includes('user.')), 'user project only')
  await loginUser(page)
  for (const path of ['/home', '/works', '/tasks', '/usage', '/subscription', '/account', '/mcp-tokens', '/login-logs']) {
    await page.goto(path)
    await expect(page.locator('body')).not.toContainText('额度过期')
    await expect(page.locator('body')).not.toContainText('额度刷新')
    await expect(page.locator('body')).not.toContainText('TokenHub')
    await expect(page.locator('body')).not.toContainText('resource not found')
    await expect(page.locator('body')).not.toContainText('我的登录日志')
  }
  await page.goto('/login-logs')
  await expect(page.getByRole('heading', { name: '登录日志' })).toBeVisible()
  await page.goto('/subscription')
  await expect(page.getByText('免费档无需支付')).toBeVisible()
  await page.goto('/mcp-tokens')
  await expect(page.getByRole('button', { name: '创建令牌' })).toBeVisible()
})

test('admin pages render operational copy', async ({ page, baseURL }) => {
  test.skip(!adminPassword || !adminName, 'SHORT_ADMIN credentials are not set')
  test.skip(!(baseURL?.includes(':8081') || baseURL?.includes('admin.')), 'admin project only')
  await loginAdmin(page)
  await expect(page.getByText('待审核')).toBeVisible()
  await expect(page.getByText('PLATFORM_ADMIN')).toHaveCount(0)
  for (const path of ['/support', '/reviews', '/creators', '/credit-packs', '/subscription-plans', '/pay-merchants', '/ai-models', '/ai-secrets', '/usage-cost', '/quality-templates', '/login-logs']) {
    const response = await page.goto(path)
    expect(response?.status()).toBeLessThan(500)
    await expect(page.locator('body')).not.toContainText('sys_secret')
    await expect(page.locator('body')).not.toContainText('application-*.properties')
  }
  await page.goto('/usage-cost')
  await expect(page.getByText('总成本')).toBeVisible()
  await page.goto('/reviews')
  await expect(page.getByText('共').first()).toBeVisible()
  await page.goto('/no-such-admin-page')
  await expect(page.getByRole('heading', { name: '页面不存在' })).toBeVisible()
})
