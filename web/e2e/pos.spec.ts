import { expect, test } from '@playwright/test'

test('pos combined payment closes shift and refunds', async ({ page }) => {
  const cashier = `E2E收银员${Date.now()}`
  await page.goto('http://localhost:5274')
  await page.getByRole('button', { name: '收银台' }).click()
  await expect(page.locator('.product-chip').first()).toBeVisible()

  await page.getByPlaceholder('收银员姓名').fill(cashier)
  await page.getByRole('button', { name: '开班' }).click()
  await expect(page.getByText('班次已开启')).toBeVisible()

  await page.locator('.product-chip', { hasText: '有机鲜牛奶' }).click()
  await page.locator('.product-chip', { hasText: '有机鲜牛奶' }).click()
  await page.locator('.product-chip', { hasText: '有机鲜牛奶' }).click()
  await page.locator('.product-chip', { hasText: '有机鲜牛奶' }).click()
  await page.locator('.product-chip', { hasText: '手工陶瓷马克杯' }).click()
  await page.getByRole('button', { name: '开单' }).click()
  await expect(page.locator('.sale-title strong')).toContainText('POS-')

  await page.getByPlaceholder('会员手机号 / 会员编号').fill('13800000002')
  await page.getByRole('button', { name: '识别会员' }).click()
  await expect(page.locator('.member-card')).toContainText('李静')

  await page.locator('.benefit-box .el-select').click()
  await page.locator('.el-select-dropdown__item', { hasText: 'POS10' }).click()
  await page.getByRole('button', { name: '核销优惠券' }).click()
  await expect(page.getByText(/优惠券已核销，优惠 ¥10\.00/)).toBeVisible()

  await page.locator('.points-row input').fill('100')
  await page.locator('.points-row input').press('Tab')
  await page.getByRole('button', { name: '积分抵扣' }).click()
  await expect(page.getByText(/积分抵扣 ¥1\.00/)).toBeVisible()

  await page.locator('.tender-amount input').fill('50')
  await page.locator('.tender-amount input').press('Tab')
  await page.locator('.tender-buttons button', { hasText: '现金' }).click()
  await page.getByRole('button', { name: '确认收款' }).click()
  await expect(page.getByText('现金收款成功')).toBeVisible()

  await page.locator('.tender-buttons button', { hasText: '微信扫码' }).click()
  await expect(page.getByText('微信扫码收款成功')).toBeVisible()
  await expect(page.getByText('还需收款')).toHaveCount(0)

  await page.getByRole('button', { name: '完成销售' }).click()
  await expect(page.getByText(/销售完成/)).toBeVisible()

  await page.getByRole('button', { name: '退款', exact: true }).click()
  const refundDialog = page.getByRole('dialog', { name: '按商品退款' })
  const milkRow = refundDialog.locator('.el-table__row', { hasText: '有机鲜牛奶' })
  await milkRow.locator('input').fill('1')
  await milkRow.locator('input').press('Tab')
  await refundDialog.locator('textarea').fill('E2E 部分退款')
  await refundDialog.getByRole('button', { name: '确认退款' }).click()
  await expect(page.getByText(/已部分退款/)).toBeVisible()

  await page.getByRole('button', { name: '退款', exact: true }).click()
  await refundDialog.getByRole('button', { name: '全部可退' }).click()
  await refundDialog.locator('textarea').fill('E2E 剩余退款')
  await refundDialog.getByRole('button', { name: '确认退款' }).click()
  await expect(page.getByText(/已全部退款/)).toBeVisible()

  await page.getByRole('button', { name: '关班' }).click()
  await page.getByRole('button', { name: '确认关班' }).click()
  await expect(page.getByText(/关班完成/)).toBeVisible()
  await expect(page.locator('.shift-summary')).toContainText('已关班')
})
