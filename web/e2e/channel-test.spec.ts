import { expect, test } from '@playwright/test'
import { login } from './env'

test('admin runs channel self-check from settings page', async ({ page }) => {
  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('系统设置', { exact: true }).click()
  await expect(page.getByText('渠道自检')).toBeVisible()

  await page.getByRole('button', { name: '发送测试消息' }).click()
  // mock 渠道在 dev 环境必然成功
  await expect(page.getByText('渠道可用：测试消息已发送')).toBeVisible({ timeout: 10_000 })
})
