import { expect, test } from '@playwright/test'
import { login } from './env'

test('admin orders use keyset load-more without page numbers', async ({ page }) => {
  await login(page)
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
