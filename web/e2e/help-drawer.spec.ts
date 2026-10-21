import { expect, test } from '@playwright/test'

test('admin help drawer shows steps, tips and pitfalls per page', async ({ page }) => {
  await page.goto('http://localhost:5273/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
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
