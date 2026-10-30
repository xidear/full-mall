import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('admin ai page explains community build without ai module', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('AI 客服', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: 'AI 客服' })).toBeVisible()

  // 社区版构建：明确说明不含该模块，而不是报错或白屏
  await expect(page.getByText('当前是社区版构建，不包含 AI 模块')).toBeVisible({ timeout: 10_000 })
})
