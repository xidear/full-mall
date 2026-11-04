import { expect, test } from '@playwright/test'
import { login } from './env'


test('after-sales and reviews use keyset load-more', async ({ page }) => {
  await login(page)

  await page.locator('.admin-menu').getByText('售后处理', { exact: true }).click()
  await expect(page.getByTestId('aftersale-load-more')).toBeVisible()

  await page.locator('.admin-menu').getByText('评价审核', { exact: true }).click()
  await expect(page.getByTestId('review-load-more')).toBeVisible()
})
