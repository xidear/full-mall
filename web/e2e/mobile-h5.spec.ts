import { expect, test } from '@playwright/test'

const MOBILE = 'http://localhost:5173'
const API = 'http://localhost:8080'

/** 移动端 H5 用户端真实联调：首页 → 详情 → 加购 → 下单 → 看到订单号。 */
test('mobile h5 browses products, adds to cart and checks out', async ({ page, request }) => {
  // 先确认后端有商品与库存
  const products = await (await request.get(`${API}/api/v1/products?size=5`)).json()
  expect(products.items?.length, '后端需要有商品数据').toBeGreaterThan(0)

  await page.setViewportSize({ width: 390, height: 844 })
  await page.goto(MOBILE)
  await expect(page.getByText('Full Mall').first()).toBeVisible()

  // 等列表渲染完再点（uni H5 首屏较慢）
  const productCard = page.locator('.card').filter({ hasText: '¥' }).first()
  await expect(productCard).toBeVisible({ timeout: 20_000 })
  await productCard.click()
  await expect(page).toHaveURL(/pages\/product\/detail/, { timeout: 20_000 })
  await expect(page.getByText('加入购物车', { exact: true })).toBeVisible({ timeout: 20_000 })

  await page.getByText('加入购物车', { exact: true }).click()
  await expect(page.getByText('已加入购物车')).toBeVisible({ timeout: 10_000 })

  await page.getByText('去购物车', { exact: true }).click()
  await expect(page.getByText(/合计/)).toBeVisible({ timeout: 15_000 })

  await page.getByText('去下单', { exact: true }).click()
  await expect(page.getByText('填写订单', { exact: true })).toBeVisible({ timeout: 15_000 })

  // uni-app H5 的 placeholder 在自定义元素上，直接按输入顺序填
  const fields = page.locator('uni-input input')
  await expect(fields).toHaveCount(6)
  const phone = `137${String(Date.now()).slice(-8)}`
  await fields.nth(0).fill('移动端验证')
  await fields.nth(1).fill(phone)
  await fields.nth(2).fill('上海市')
  await fields.nth(3).fill('上海市')
  await fields.nth(4).fill('静安区')
  await fields.nth(5).fill('测试路 1 号')

  // 该手机号需先 OTP 验证，否则后端明确返回 CHECKOUT_OTP_REQUIRED
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  expect(issued.ok()).toBeTruthy()
  const code = (await issued.json()).debugCode
  await request.post(`${API}/api/v1/member/auth/verify`, { data: { phone, code } })

  await page.getByText('提交订单', { exact: true }).click()
  await expect(page.getByText('下单成功')).toBeVisible({ timeout: 20_000 })
  await expect(page.getByText(/订单号：FM/)).toBeVisible()
})
