import { defineConfig, devices } from '@playwright/test'

const userBase = process.env.USER_BASE_URL || 'http://127.0.0.1:8082'
const adminBase = process.env.ADMIN_BASE_URL || 'http://127.0.0.1:8081'

export default defineConfig({
  testDir: './tests',
  timeout: 45000,
  expect: { timeout: 10000 },
  retries: 0,
  workers: 1,
  reporter: [['list']],
  use: { trace: 'off' },
  projects: [
    { name: 'user-desktop', use: { ...devices['Desktop Chrome'], baseURL: userBase } },
    {
      name: 'user-mobile',
      use: { ...devices['Desktop Chrome'], viewport: { width: 390, height: 844 }, baseURL: userBase },
    },
    { name: 'admin-desktop', use: { ...devices['Desktop Chrome'], baseURL: adminBase } },
  ],
})
