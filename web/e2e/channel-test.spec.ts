import { expect, test } from '@playwright/test'

test('admin runs channel self-check from settings page', async ({ page }) => {
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('系统设置', { exact: true }).click()
  await expect(page.getByText('渠道自检')).toBeVisible()

  await page.getByRole('button', { name: '发送测试消息' }).click()
  // mock 渠道在 dev 环境必然成功
  await expect(page.getByText('渠道可用：测试消息已发送')).toBeVisible({ timeout: 10_000 })
})
