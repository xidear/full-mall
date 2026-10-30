import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('admin opens review moderation page and filters by status', async ({ page }) => {
  // 说明：评价只能由顾客在已履约订单后提交，因此本用例只验证页面加载与筛选可用；
  // 隐藏/恢复的业务正确性由后端 ReviewModerationTest 覆盖。

  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('评价审核', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '评价审核', level: 2 })).toBeVisible()
  await expect(page.getByText(/共 \d+ 条评价/)).toBeVisible()

  await page.locator('.header-actions .el-select').click()
  await page.getByRole('option', { name: '已隐藏' }).click()
  await expect(page.getByText(/共 \d+ 条评价/)).toBeVisible()
})