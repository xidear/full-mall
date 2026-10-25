import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'

test('member pages require login and render once signed in', async ({ page, request }) => {
  const phone = `135${String(Date.now()).slice(-8)}`
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const token = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token

  // 未登录：提示登录
  await page.goto('http://localhost:41285/notifications')
  await expect(page.getByText('登录后可查看消息。')).toBeVisible()

  // 登录后：页面正常渲染（空列表也算通过）
  await page.context().addCookies([{
    name: 'fullmall_member_token', value: token, domain: 'localhost', path: '/', sameSite: 'Lax'
  }])
  await page.goto('http://localhost:41285/notifications')
  await expect(page.getByRole('heading', { name: '消息中心' })).toBeVisible()
  await expect(page.getByText('暂时没有消息。')).toBeVisible()

  await page.goto('http://localhost:41285/favorites')
  await expect(page.getByRole('heading', { name: '我的收藏' })).toBeVisible()
  await expect(page.getByText(/还没有收藏|加载中/)).toBeVisible()
})
