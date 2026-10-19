import { test, expect } from '@playwright/test'

const LEGAL = [
  'terms-of-service',
  'privacy-policy',
  'personal-information-inventory',
  'third-party-sharing',
  'app-permissions',
]

test.describe('document routes', () => {
  test('unknown path returns the SPA shell instead of JSON', async ({ request, page }) => {
    const response = await request.get('/no-such-page-qa', { headers: { Accept: 'text/html' } })
    expect(response.status()).toBe(200)
    expect(response.headers()['content-type'] || '').toContain('text/html')
    const body = await response.text()
    expect(body).not.toContain('"code":"404000"')
    expect(body.toLowerCase()).not.toContain('whitelabel')
    await page.goto('/no-such-page-qa')
    await expect(page.getByRole('heading', { name: '页面不存在' })).toBeVisible()
  })

  test('deep link and refresh keep the SPA', async ({ page }) => {
    const response = await page.goto('/works/qa-missing-work/script')
    expect(response?.status()).toBeLessThan(500)
    expect(response?.headers()['content-type'] || '').toContain('text/html')
    await page.reload()
    await expect(page.locator('body')).not.toContainText('resource not found')
  })

  test('login page lists five bilingual legal links', async ({ page }) => {
    await page.goto('/login')
    for (const slug of LEGAL) {
      await expect(page.locator(`a[href="https://www.bgssai.com/${slug}/"]`).first()).toBeVisible()
      await expect(page.locator(`a[href="https://www.bgssai.com/en/${slug}/"]`).first()).toBeVisible()
    }
    await expect(page.locator('body')).not.toContainText('苏ICP')
  })
})
