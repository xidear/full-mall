import { expect, test } from '@playwright/test'

test('dashboard todo item navigates to filtered orders', async ({ page }) => {
  await page.goto('http://localhost:5273/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const nums = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(nums[0] + nums[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.todo-card', { hasText: '待发货' }).first().click()

  await expect(page).toHaveURL(/\/orders\?status=PENDING_SHIPMENT/)
  await expect(page.getByRole('main').getByRole('heading', { name: '订单管理' })).toBeVisible()
})
