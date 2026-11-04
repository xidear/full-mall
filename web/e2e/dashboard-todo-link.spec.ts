import { expect, test } from '@playwright/test'
import { login } from './env'

test('dashboard todo item navigates to filtered orders', async ({ page }) => {
  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.todo-card', { hasText: '待发货' }).first().click()

  await expect(page).toHaveURL(/\/orders\?status=PENDING_SHIPMENT/)
  await expect(page.getByRole('main').getByRole('heading', { name: '订单管理' })).toBeVisible()
})
