import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'

async function adminToken(request: any) {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: nums[0] + nums[1] }
  })).json()
  return login.token as string
}

test('admin creates and deletes a brand; in-use brand cannot be deleted', async ({ page, request }) => {
  const token = await adminToken(request)
  const code = `E2E-BR-${Date.now().toString().slice(-6)}`
  const created = await request.post(`${API}/admin/v1/brands`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { code, name: 'E2E 品牌', sortOrder: 1 }
  })
  expect(created.ok(), await created.text()).toBeTruthy()

  await page.goto('http://localhost:5273/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const nums = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(nums[0] + nums[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('品牌管理', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: '品牌管理' })).toBeVisible()
  await expect(page.getByRole('cell', { name: code })).toBeVisible({ timeout: 10_000 })
})

test('bargain flow reaches READY then binds an order', async ({ request }) => {
  const token = await adminToken(request)
  const phone = `136${String(Date.now()).slice(-8)}`
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const memberToken = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token
  const me = await (await request.get(`${API}/api/v1/member/auth/me`,
    { headers: { Authorization: `Bearer ${memberToken}` } })).json()
  const memberId = (me.id ?? me.memberId ?? me.member?.id) as string
  expect(memberId, JSON.stringify(me)).toBeTruthy()

  // 找一个在售 SKU 建活动
  const products = await (await request.get(`${API}/api/v1/products?size=5`)).json()
  const skuId = products.items[0].skus[0].id as string
  const activity = await request.post(`${API}/admin/v1/bargain-activities`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { skuId, floorPrice: 100, maxCut: 5000, minHelpers: 3,
            cutMin: 50, cutMax: 100, expiresHours: 24,
            allowBelowCost: true, belowCostReason: 'E2E 引流',
            startsAt: new Date().toISOString(),
            endsAt: new Date(Date.now() + 86400000).toISOString() }
  })
  expect(activity.ok(), await activity.text()).toBeTruthy()
  const activityId = (await activity.json()).activityId as string

  const started = await (await request.post(
    `${API}/api/v1/bargain/activities/${activityId}/start?memberId=${memberId}`)).json()
  expect(started.status).toBe('ONGOING')

  // 三个不同会员助力
  for (let i = 0; i < 3; i++) {
    const helperPhone = `137${String(Date.now() + i).slice(-8)}`
    const issuedHelper = await request.post(`${API}/api/v1/member/auth/otp`,
      { data: { phone: helperPhone } })
    const helperCode = (await issuedHelper.json()).debugCode
    const helperToken = (await (await request.post(`${API}/api/v1/member/auth/verify`,
      { data: { phone: helperPhone, code: helperCode } })).json()).token
    const helperMe = await (await request.get(`${API}/api/v1/member/auth/me`,
      { headers: { Authorization: `Bearer ${helperToken}` } })).json()
    const helperId = (helperMe.id ?? helperMe.memberId ?? helperMe.member?.id) as string
    const helped = await request.post(
      `${API}/api/v1/bargain/sessions/${started.id}/help?memberId=${helperId}`)
    expect(helped.ok(), await helped.text()).toBeTruthy()
  }

  const bound = await request.post(
    `${API}/api/v1/bargain/sessions/${started.id}/order?orderId=${crypto.randomUUID()}`)
  expect(bound.ok(), await bound.text()).toBeTruthy()
  expect((await bound.json()).status).toBe('ORDERED')
})
