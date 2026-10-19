import { test, expect } from '@playwright/test'

test('bot download page has no developer copy', async ({ page, baseURL }) => {
  test.skip(!(baseURL?.includes(':8082') || baseURL?.includes('user.')), 'user site only')
  await page.goto('/download/bot')
  await expect(page.locator('body')).not.toContainText('对标')
  await expect(page.locator('body')).not.toContainText('放置说明')
  const body = await page.locator('body').innerText()
  expect(body.includes('安装包即将上线') || body.includes('下载')).toBeTruthy()
})
