import { expect, test } from '@playwright/test'
import { API_BASE, STOREFRONT_BASE, STOREFRONT_HOST } from './env'

const API = API_BASE
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'
const PHONE = `137${String(Date.now()).slice(-8)}`

test('customer sees tracking on order page after shipment', async ({ page, request }) => {
  // 1) 会员身份（OTP）
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone: PHONE } })
  expect(issued.ok(), await issued.text()).toBeTruthy()
  const code = (await issued.json()).debugCode
  const session = await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone: PHONE, code } })
  const token = (await session.json()).token

  // 2) 下单 + 支付
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `tracking-${Date.now()}`, Authorization: `Bearer ${token}` },
    data: {
      items: [{ skuId: SKU, quantity: 1 }],
      fulfillmentMode: 'SHIP',
      contact: { name: '轨迹验证', phone: PHONE },
      address: { countryCode: 'CN', province: '上海市', city: '上海市', district: '静安区', detail: '测试路 1 号' },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  // 3) 员工发货（带运单号）
  const adminCaptcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const numbers = (adminCaptcha.question.match(/\d+/g) ?? []).map(Number)
  const adminLogin = await request.post(`${API}/admin/v1/auth/login`, {
    data: {
      username: 'admin', password: 'Admin@123456',
      captchaId: adminCaptcha.captchaId, captchaAnswer: numbers[0] + numbers[1]
    }
  })
  const adminToken = (await adminLogin.json()).token
  const fulfillments = await request.get(`${API}/store/v1/orders/${order.id}/fulfillments`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  expect(fulfillments.ok(), await fulfillments.text()).toBeTruthy()
  const fulfillmentList = await fulfillments.json()
  const fulfillmentId = fulfillmentList[0].id
  const accept = await request.post(`${API}/store/v1/fulfillments/${fulfillmentId}/accept`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  expect(accept.ok(), await accept.text()).toBeTruthy()
  const pack = await request.post(`${API}/store/v1/fulfillments/${fulfillmentId}/pack`, {
    headers: { Authorization: `Bearer ${adminToken}` }
  })
  expect(pack.ok(), await pack.text()).toBeTruthy()
  const ship = await request.post(`${API}/store/v1/fulfillments/${fulfillmentId}/ship`, {
    headers: { Authorization: `Bearer ${adminToken}` },
    data: { carrierCode: 'SF', trackingNo: 'SF-E2E-TRACK-1' }
  })
  expect(ship.ok(), await ship.text()).toBeTruthy()

  // 4) 顾客打开订单页（cookie 带身份，SSR 也要能取到）并查询轨迹
  await page.context().addCookies([{
    name: 'fullmall_member_token', value: token,
    domain: STOREFRONT_HOST, path: '/', sameSite: 'Lax'
  }])
  await page.goto(`${STOREFRONT_BASE}/orders/${order.id}`)
  // D11 验证：硬刷新（SSR）也能拿到会员身份并渲染履约
  await expect(page.getByRole('heading', { name: '履约' })).toBeVisible({ timeout: 15_000 })
  await expect(page.getByText('物流单号：SF-E2E-TRACK-1')).toBeVisible()
  await page.getByRole('button', { name: '查看物流轨迹' }).click()
  await expect(page.getByRole('heading', { name: '物流轨迹' })).toBeVisible({ timeout: 15_000 })
  // 承运商可能尚未回传：要么有轨迹，要么明确提示"暂无"，都不能是空白
  await expect(page.getByText(/已揽收|暂无物流轨迹|暂时不可用/)).toBeVisible()
})
