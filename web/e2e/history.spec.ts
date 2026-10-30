import { expect, test } from '@playwright/test'
import { e2ePhone } from './utils/e2ePhone'
import { API_BASE, STOREFRONT_BASE, STOREFRONT_HOST } from './env'

const API = API_BASE
const SKU = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb'

test('browse history page shows recorded product', async ({ page, request }) => {
  const phone = e2ePhone()
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const token = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token

  const recorded = await request.post(`${API}/api/v1/history/${SKU}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(recorded.ok(), await recorded.text()).toBeTruthy()

  const historyResponse = await request.get(`${API}/api/v1/history`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  const list = await historyResponse.json()
  expect(list.length).toBe(1)
  const title = list[0].title as string
  expect(title.length).toBeGreaterThan(0)

  await page.context().addCookies([{
    name: 'fullmall_member_token', value: token, domain: STOREFRONT_HOST, path: '/', sameSite: 'Lax'
  }])
  await page.goto(STOREFRONT_BASE + '/history')
  await expect(page.getByRole('heading', { name: '浏览足迹' })).toBeVisible()
  await expect(page.getByRole('link', { name: title })).toBeVisible()
  await expect(page.getByText(/看过 \d+ 次/)).toBeVisible()
})