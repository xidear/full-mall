import { defineConfig, devices } from '@playwright/test'

/**
 * Storefront 只读浏览器审计专用配置。
 *
 * 仓库根配置 web/playwright.config.ts 的 testDir 是 ./e2e，
 * 直接 `npx playwright test audit/storefront-audit.spec.ts` 会报 "No tests found"。
 * 因此用本配置把 testDir 指向 audit/，不修改原配置。
 *
 *   cd web && npx playwright test --config=audit/playwright.audit.config.ts
 */
export default defineConfig({
  testDir: '.',
  timeout: 90_000,
  expect: { timeout: 6_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: process.env.STOREFRONT_BASE_URL || 'http://localhost:41285',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    }
  ]
})
