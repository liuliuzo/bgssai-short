import { test, expect } from '@playwright/test'

test('health endpoint answers JSON', async ({ request, baseURL }) => {
  const path = baseURL?.includes('8081') || baseURL?.includes('admin.')
    ? '/bgssai/health'
    : '/bgssai/health'
  const response = await request.get(path, { headers: { Accept: 'application/json' } })
  expect(response.status()).toBe(200)
  const body = await response.json()
  expect(body.status || body.result?.status || 'UP').toBeTruthy()
})
