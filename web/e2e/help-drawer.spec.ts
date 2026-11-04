import { expect, test } from '@playwright/test'
import { login } from './env'

test('admin help drawer shows steps, tips and pitfalls per page', async ({ page }) => {
  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  // 订单页：帮助应给出"订单"相关内容，且三段齐全
  await page.locator('.admin-menu').getByText('订单管理', { exact: true }).click()
  await page.getByRole('button', { name: '帮助' }).click()
  const drawer = page.getByRole('dialog')
  await expect(drawer.getByText('订单管理 · 使用帮助')).toBeVisible()
  await expect(drawer.getByText('操作步骤')).toBeVisible()
  await expect(drawer.getByText('运营建议')).toBeVisible()
  await expect(drawer.getByText('常见错误')).toBeVisible()
  await expect(drawer.getByText(/部分退款会按比例冻结分销佣金/)).toBeVisible()
})
