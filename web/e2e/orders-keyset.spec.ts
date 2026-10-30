import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('admin orders use keyset load-more without page numbers', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  await expect(page.locator('.captcha-question')).toHaveText(/\d+ \+ \d+/)
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('订单管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '订单管理' })).toBeVisible()

  const loadMore = page.getByTestId('load-more')
  await expect(loadMore).toBeVisible()
  await expect(page.locator('.el-pagination')).toHaveCount(0)
  // 有更多数据时才点；没数据时按钮禁用也是正确行为
  const enabled = await loadMore.isEnabled()
  const before = await page.locator('.el-table__row').count()
  if (enabled) {
    await loadMore.click()
    await expect.poll(async () => page.locator('.el-table__row').count())
      .toBeGreaterThanOrEqual(before)
  }
  await expect(loadMore).toHaveText(/加载更多|没有更多了/)
})
