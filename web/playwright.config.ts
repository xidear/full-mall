import { defineConfig, devices } from '@playwright/test'

export default defineConfig({
  testDir: './e2e',
  // 收尾清理：只删"199 前缀且无订单"的 e2e 会员（有订单的保留，删不了也不该偷偷删）
  globalTeardown: './e2e/global-teardown.ts',
  timeout: 60_000,
  expect: { timeout: 15_000 },
  fullyParallel: false,
  workers: 1,
  reporter: [['list']],
  use: {
    baseURL: 'http://localhost:41285',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] }
    },
    {
      name: 'edge',
      use: {
        ...devices['Desktop Edge'],
        channel: 'msedge'
      }
    }
  ]
})