import { expect, test } from '@playwright/test'
import { login } from './env'

test('admin ai page renders status or community notice', async ({ page }) => {
  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('AI 客服', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: 'AI 客服' })).toBeVisible()

  // 社区版显示“不包含 AI 模块”；内部/企业版显示模型状态。两者都不能白屏。
  await expect(
    page.getByText('当前是社区版构建，不包含 AI 模块')
      .or(page.getByText('对话模型'))
      .first()
  ).toBeVisible({ timeout: 10_000 })
})
