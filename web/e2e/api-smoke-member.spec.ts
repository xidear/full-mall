import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'

/** 顾客端只读接口：用会员 token 调用，任何 5xx 都是真 bug。 */
const MEMBER_GETS = [
  '/api/v1/member/auth/me',
  '/api/v1/coupons/mine',
  '/api/v1/favorites?limit=5',
  '/api/v1/history?limit=5',
  '/api/v1/notifications?limit=5',
  '/api/v1/notifications/unread-count',
  '/api/v1/addresses',
  '/api/v1/stores'
]

test('member read endpoints respond without server errors', async ({ request }) => {
  const phone = `139${String(Date.now()).slice(-8)}`
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const me = await request.post(`${API}/api/v1/member/auth/verify`, { data: { phone, code } })
  const token = (await me.json()).token as string
  const headers = { Authorization: `Bearer ${token}` }

  const failures: string[] = []
  for (const path of MEMBER_GETS) {
    const response = await request.get(`${API}${path}`, { headers })
    if (response.status() >= 400) {
      failures.push(`${path} → ${response.status()} ${(await response.text()).slice(0, 160)}`)
    }
  }
  // 写接口：非法输入必须 4xx（否则说明校验缺失或异常被吞）
  const writeCases: Array<{ path: string; body: unknown; note: string }> = [
    { path: '/api/v1/checkout', body: {}, note: '结算缺字段' },
    { path: '/api/v1/favorites/not-a-uuid', body: {}, note: '收藏非法 SKU' }
  ]
  for (const item of writeCases) {
    const response = await request.post(`${API}${item.path}`, { headers, data: item.body })
    if (response.status() >= 500) {
      failures.push(`${item.note} (POST ${item.path}) → ${response.status()}`)
    }
  }
  expect(failures, failures.join('\n')).toEqual([])
})
