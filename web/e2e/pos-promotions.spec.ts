import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('admin creates a POS-only promotion and sees it in the filtered list', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  await expect(page.locator('.captcha-question')).toHaveText(/\d+ \+ \d+/)
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('POS 专属促销', { exact: true }).click()
  await expect(page.locator('h1', { hasText: 'POS 专属促销' }).first()).toBeVisible()
  await expect(page.getByText('只在门店收银生效')).toBeVisible()

  const code = `POS-E2E-${Date.now()}`
  const field = (id: string) => page.locator(`[data-testid="${id}"] input, input[data-testid="${id}"]`).first()
  await field('pos-code').fill(code)
  await field('pos-title').fill('门店满减 E2E')
  await field('pos-threshold').fill('100')
  await field('pos-value').fill('10')

  // 起止时间已有默认值（现在 ~ 一天后），无需操作日期控件
  await page.getByRole('button', { name: '创建 POS 促销' }).click()
  await expect(page.getByText('已创建 POS 促销')).toBeVisible()
  await expect(page.locator('.el-table').getByText(code)).toBeVisible()
})
