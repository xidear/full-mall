import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('admin can open audit log and filter by event type', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('操作审计', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '操作审计', level: 2 })).toBeVisible()
  await expect(page.getByText(/共 \d+ 条/)).toBeVisible()

  // 事件类型已改为后端枚举下拉，禁止手填
  await page.locator('.el-select').first().click()
  await page.locator('.el-select-dropdown__item').filter({ hasText: '支付退款' }).first().click()
  await page.getByRole('button', { name: '查询' }).click()
  await expect(page.getByText(/共 \d+ 条/)).toBeVisible()
})
