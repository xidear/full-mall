import { expect, test } from '@playwright/test'
import { e2ePhone } from './utils/e2ePhone'
import { API_BASE, login } from './env'

const API = API_BASE

async function adminToken(request: any) {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: nums[0] + nums[1] }
  })).json()
  return login.token as string
}

// 守住两条不变量：品牌名唯一（重名返回 409 + BRAND_NAME_DUPLICATED）、
// 砍价必须满 minHelpers 才能把会话绑到订单（状态机 ONGOING → ORDERED）。
// 用例自己清理数据：历史上没清理时累积过 24 条同名“E2E 品牌”，会污染演示库。
test('admin creates a brand, duplicates rejected by name, then cleans up', async ({ page, request }) => {
  const token = await adminToken(request)
  const code = `E2E-BR-${Date.now().toString().slice(-6)}`
  // 名字带随机后缀：品牌名唯一，固定名字会让多次运行互相冲突（测试自身要能重复跑）
  const name = `E2E 品牌 ${code.slice(-6)}`
  const created = await request.post(`${API}/admin/v1/brands`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { code, name, sortOrder: 1 }
  })
  expect(created.ok(), await created.text()).toBeTruthy()

  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('品牌管理', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: '品牌管理' })).toBeVisible()
  await expect(page.getByRole('cell', { name: code })).toBeVisible({ timeout: 10_000 })

  // 同名品牌不允许重复创建（品牌名唯一）——必须在删除之前校验，否则校验的是"已删掉的名字"
  const duplicate = await request.post(`${API}/admin/v1/brands`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { code: `${code}-DUP`, name, sortOrder: 1 }
  })
  expect(duplicate.status()).toBe(409)
  expect(await duplicate.text()).toContain('BRAND_NAME_DUPLICATED')

  // 测试自己清理：名字与编码都不会复用，留着会污染演示库（曾累积 24 条同名"E2E 品牌"）
  const listed = await request.get(`${API}/admin/v1/brands`, { headers: { Authorization: `Bearer ${token}` } })
  const brands = await listed.json() as Array<{ id: string; code: string }>
  const createdBrand = brands.find(brand => brand.code === code)
  expect(createdBrand, '新建的品牌应出现在列表里').toBeTruthy()
  const removed = await request.delete(`${API}/admin/v1/brands/${createdBrand!.id}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(removed.ok(), await removed.text()).toBeTruthy()
})

// 纯 API 用例（不开浏览器）：验证砍价最小闭环。
// 依赖后端 dev 会把验证码放在 debugCode 里返回；生产关掉该字段后本用例会失败。
test('bargain flow reaches READY then binds an order', async ({ request }) => {
  const token = await adminToken(request)
  const phone = e2ePhone()
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