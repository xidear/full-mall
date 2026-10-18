import { expect, test } from '@playwright/test'

test('admin can load dashboard, products, inventory and theme studio', async ({ page }) => {
  await page.goto('http://localhost:5273/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  await expect(page.locator('.captcha-question')).toHaveText(/\d+ \+ \d+/)
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.getByText('商品管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '商品管理' })).toBeVisible()
  await expect(page.getByText('有机鲜牛奶').first()).toBeVisible()

  await page.getByText('订单管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '订单管理' })).toBeVisible()

  await page.getByText('客户管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '客户管理' })).toBeVisible()

  await page.getByText('员工与权限', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '员工与权限' })).toBeVisible()

  await page.getByText('库存与批次', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '库存批次' })).toBeVisible()
  await expect(page.getByText('MILK-WH-20261015').first()).toBeVisible()

  await page.getByText('通知中心', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '通知中心' })).toBeVisible()

  await page.getByText('系统设置', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '系统设置' })).toBeVisible()

  await page.getByText('主题配置', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '主题工作室' })).toBeVisible()
  await page.getByRole('button', { name: '机械模板' }).click()
  await expect(page.getByText('工业级选型 · 稳定交付')).toBeVisible()
})