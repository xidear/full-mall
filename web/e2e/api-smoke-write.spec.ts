import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'

/**
 * 写接口冒烟：**全部使用明显无效的参数**，让请求在校验层就被拒绝。
 * 目的：确认"非法输入返回 4xx 而不是 500"——500 意味着服务端崩了或吞了异常。
 */
const WRITE_CASES: Array<{ method: 'POST' | 'PUT'; path: string; body?: unknown; note: string }> = [
  { method: 'POST', path: '/admin/v1/promotions/rules', body: {}, note: '促销规则缺字段' },
  { method: 'POST', path: '/admin/v1/coupons/templates', body: { code: '' }, note: '券模板缺字段' },
  { method: 'POST', path: '/admin/v1/supplier-returns',
    body: { supplierId: 'not-a-uuid', orderId: 'not-a-uuid' }, note: '供应商退货非法 UUID' },
  { method: 'POST', path: '/admin/v1/reviews/00000000-0000-0000-0000-000000000000/status',
    body: { status: 'DELETED' }, note: '评价状态非法' },
  { method: 'POST', path: '/admin/v1/orders/00000000-0000-0000-0000-000000000000/refunds',
    body: { amount: 100, reason: 'smoke' }, note: '退款订单不存在' },
  { method: 'POST', path: '/admin/v1/notifications/00000000-0000-0000-0000-000000000000/read',
    note: '通知不存在' },
  { method: 'PUT', path: '/admin/v1/settings', body: { 'not.allowed.key': 'x' }, note: '非法设置键' },
  { method: 'POST', path: '/admin/v1/distribution/withdrawals',
    body: { promoterId: 'not-a-uuid', amount: -1 }, note: '提现非法参数' }
]

test('write endpoints reject invalid input with 4xx, never 500', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const headers = { Authorization: `Bearer ${login.token}` }

  const failures: string[] = []
  for (const item of WRITE_CASES) {
    const response = item.method === 'POST'
      ? await request.post(`${API}${item.path}`, { headers, data: item.body })
      : await request.put(`${API}${item.path}`, { headers, data: item.body })
    if (response.status() >= 500) {
      failures.push(`${item.note} (${item.method} ${item.path}) → ${response.status()}`
        + ` ${(await response.text()).slice(0, 200)}`)
    }
  }
  expect(failures, failures.join('\n')).toEqual([])
})
