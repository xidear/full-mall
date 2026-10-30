import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

async function login(page: import('@playwright/test').Page) {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  await expect(page.locator('.captcha-question')).toHaveText(/\d+ \+ \d+/)
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()
}

test('after-sales and reviews use keyset load-more', async ({ page }) => {
  await login(page)

  await page.locator('.admin-menu').getByText('售后处理', { exact: true }).click()
  await expect(page.getByTestId('aftersale-load-more')).toBeVisible()

  await page.locator('.admin-menu').getByText('评价审核', { exact: true }).click()
  await expect(page.getByTestId('review-load-more')).toBeVisible()
})
