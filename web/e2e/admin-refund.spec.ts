import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const PHONE = `136${String(Date.now()).slice(-8)}`

test('admin refunds part of an order from the order detail', async ({ page, request }) => {
  // 造一笔已支付订单（顾客身份 OTP）
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone: PHONE } })
  const code = (await issued.json()).debugCode
  const token = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone: PHONE, code } })).json()).token
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `refund-${Date.now()}`, Authorization: `Bearer ${token}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'PICKUP',
      storeId: '22222222-2222-2222-2222-222222222222',
      contact: { name: '退款验证', phone: PHONE },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  // 后台登录并打开该订单
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('订单管理', { exact: true }).click()
  await page.getByRole('button', { name: '查看' }).first().click()
  await expect(page.getByRole('button', { name: '发起退款' })).toBeVisible()

  await page.getByRole('button', { name: '发起退款' }).click()
  const dialog = page.getByRole('dialog', { name: '发起退款' })
  await dialog.getByPlaceholder('必填，会写入审计').fill('E2E 部分退款')
  // 金额改成订单金额的一半
  const amount = dialog.locator('input[type="number"]')
  const original = Number(await amount.inputValue())
  await amount.fill(String(Math.max(0.01, Math.round(original / 2 * 100) / 100)))
  await dialog.getByRole('button', { name: '确认退款' }).click()
  await expect(page.getByText('退款已提交')).toBeVisible({ timeout: 10_000 })
})
