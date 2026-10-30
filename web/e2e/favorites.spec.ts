import { expect, test } from '@playwright/test'
import { e2ePhone } from './utils/e2ePhone'
import { API_BASE, STOREFRONT_BASE, STOREFRONT_HOST } from './env'

const API = API_BASE
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'

test('favorite added by member shows on favorites page', async ({ page, request }) => {
  const phone = e2ePhone()
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const token = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token

  const added = await request.post(`${API}/api/v1/favorites/${SKU}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(added.ok(), await added.text()).toBeTruthy()

  const list = await request.get(`${API}/api/v1/favorites`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  const items = await list.json()
  expect(items.length).toBe(1)
  const title = items[0].title as string
  expect(title.length).toBeGreaterThan(0)

  await page.context().addCookies([{
    name: 'fullmall_member_token', value: token, domain: STOREFRONT_HOST, path: '/', sameSite: 'Lax'
  }])
  await page.goto(STOREFRONT_BASE + '/favorites')
  await expect(page.getByRole('heading', { name: '我的收藏' })).toBeVisible()
  // 标题与价格都要渲染（此前接口只返回 skuId，页面无法展示）
  await expect(page.getByRole('link', { name: title })).toBeVisible()
  await expect(page.getByText(/¥|￥/)).toBeVisible()
})