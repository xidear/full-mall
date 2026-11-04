import { expect, test } from '@playwright/test'
import { login } from './env'

test('admin opens review moderation page and filters by status', async ({ page }) => {
  // 说明：评价只能由顾客在已履约订单后提交，因此本用例只验证页面加载与筛选可用；
  // 隐藏/恢复的业务正确性由后端 ReviewModerationTest 覆盖。

  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('评价审核', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '评价审核', level: 2 })).toBeVisible()
  await expect(page.getByText(/共 \d+ 条评价/)).toBeVisible()

  await page.locator('.header-actions .el-select').click()
  await page.getByRole('option', { name: '已隐藏' }).click()
  await expect(page.getByText(/共 \d+ 条评价/)).toBeVisible()
})