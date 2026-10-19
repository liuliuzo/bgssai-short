import { test, expect } from '@playwright/test'

const userPassword = process.env.SHORT_USER_PASSWORD
const adminPassword = process.env.SHORT_ADMIN_PASSWORD
const userName = process.env.SHORT_USER
const adminName = process.env.SHORT_ADMIN

test('protected user page returns to the original path after login', async ({ page, baseURL }) => {
  test.skip(!userPassword || !userName, 'SHORT_USER credentials are not set')
  test.skip(!baseURL?.includes('8082') && !baseURL?.includes('user.'), 'user project only')
  await page.goto('/works')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  await page.getByRole('tab', { name: '密码' }).click()
  await page.getByPlaceholder('手机号 / 邮箱 / 账号').fill(userName)
  await page.getByPlaceholder('请输入密码').fill(userPassword)
  const agree = page.locator('input[type="checkbox"]')
  if (await agree.count()) await agree.first().check()
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/works/, { timeout: 20000 })
})

test('protected admin page returns to the original path after login', async ({ page, baseURL }) => {
  test.skip(!adminPassword || !adminName, 'SHORT_ADMIN credentials are not set')
  test.skip(!baseURL?.includes('8081') && !baseURL?.includes('admin.'), 'admin project only')
  await page.goto('/reviews')
  await expect(page).toHaveURL(/\/login\?redirect=/)
  await page.locator('#admin-username').fill(adminName)
  await page.locator('#admin-password').fill(adminPassword)
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page).toHaveURL(/\/reviews/, { timeout: 20000 })
})
