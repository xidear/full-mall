import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'
const SUPPLIER_ID = '33333333-3333-3333-3333-333333333333'
const SUPPLIER_SKU = 'bbbb0000-0000-0000-0000-0000000000c1'

test('admin generates settlement and sees payable in dialog', async ({ page, request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const authorization = { Authorization: `Bearer ${login.token}` }

  // 造一笔已支付的供应商代发订单
  const phone = `130${String(Date.now()).slice(-8)}`
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const member = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token
  const checkout = await request.post(`${API}/api/v1/checkout`, {
    headers: { 'Idempotency-Key': `st-${Date.now()}`, Authorization: `Bearer ${member}` },
    data: {
      items: [{ skuId: SUPPLIER_SKU, quantity: 1 }],
      fulfillmentMode: 'SHIP',
      contact: { name: '结算顾客', phone },
      address: { province: '上海市', city: '上海市', district: '静安区', detail: '结算路 1 号' },
      legalAcceptanceIds: []
    }
  })
  expect(checkout.status(), await checkout.text()).toBe(201)
  const order = await checkout.json()
  expect((await request.post(`${API}/api/v1/payments/mock/${order.id}/pay`)).ok()).toBeTruthy()

  // 后台生成结算单（周期覆盖当前时间）
  const start = new Date(Date.now() - 864e5).toISOString()
  const end = new Date(Date.now() + 864e5).toISOString()
  const generated = await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/settlements?periodStart=${encodeURIComponent(start)}&periodEnd=${encodeURIComponent(end)}`,
    { headers: authorization })
  expect(generated.ok(), await generated.text()).toBeTruthy()

  // 界面：供应商页 → 结算 → 列表出现应付金额
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('供应商', { exact: true }).click()
  await page.getByRole('button', { name: '结算' }).first().click()
  await expect(page.getByRole('dialog', { name: '供应商结算' })).toBeVisible()
  await expect(page.getByText('应付')).toBeVisible()
  // 至少一行结算单：应付列有数字
  await expect(page.locator('.el-table__row').first()).toBeVisible({ timeout: 10_000 })
})
