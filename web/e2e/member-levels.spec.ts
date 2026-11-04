import { expect, test } from '@playwright/test'
import { login } from './env'

test('admin edits member level discount and it persists', async ({ page }) => {
  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('会员与优惠券', { exact: true }).click()
  await page.getByRole('tab', { name: '会员等级' }).click()
  await expect(page.getByText('等级折扣在收银绑定会员时自动生效')).toBeVisible()

  const firstRow = page.getByRole('tabpanel', { name: '会员等级' }).locator('tbody tr').first()
  await expect(firstRow).toBeVisible()
  const discountInput = firstRow.getByRole('spinbutton').first()
  const original = Number(await discountInput.inputValue())
  const target = original === 95 ? 96 : 95

  await discountInput.fill(String(target))
  await firstRow.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText(/已保存|保存成功/)).toBeVisible({ timeout: 10_000 })

  // 重新加载页面确认落库
  await page.reload()
  await page.getByRole('tab', { name: '会员等级' }).click()
  await expect(page.getByRole('tabpanel', { name: '会员等级' }).locator('tbody tr').first().getByRole('spinbutton').first())
    .toHaveValue(String(target))
})
