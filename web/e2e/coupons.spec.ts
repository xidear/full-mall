import { expect, test } from '@playwright/test'

test('admin creates and activates a coupon template', async ({ page }) => {
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('优惠券', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '优惠券', level: 2 })).toBeVisible()

  const code = `E2E-CP-${Date.now().toString().slice(-6)}`
  await page.getByRole('button', { name: '新建券模板' }).click()
  const dialog = page.getByRole('dialog', { name: '新建券模板' })
  await dialog.getByPlaceholder('例如 NEW-YEAR-50').fill(code)
  await dialog.getByPlaceholder('例如 新年满减券').fill('E2E 满减券')
  await dialog.getByRole('button', { name: '创建' }).click()

  await expect(page.getByText('券模板已创建（草稿）')).toBeVisible()
  const row = page.locator('.el-table__row', { hasText: code })
  await expect(row).toBeVisible()
  await expect(row.getByText('草稿')).toBeVisible()

  await row.getByRole('button', { name: '启用' }).click()
  await expect(page.getByText('券模板已启用')).toBeVisible({ timeout: 5000 })
  await expect(page.locator('.el-table__row', { hasText: code }).getByText('发放中'))
    .toBeVisible({ timeout: 5000 })
})
