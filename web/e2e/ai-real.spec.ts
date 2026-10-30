import { expect, test } from '@playwright/test'
import { API_BASE } from './env'

const API = API_BASE

test.skip(process.env.APP_EDITION !== 'enterprise', '仅企业版构建运行')

test('real model answers grounded questions with citations', async ({ request }) => {
  test.setTimeout(240_000)
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const headers = { Authorization: `Bearer ${login.token}` }

  const status = await (await request.get(`${API}/admin/v1/ai/status`, { headers })).json()
  expect(status.ready, JSON.stringify(status)).toBeTruthy()

  const md = [
    'Q: 鲜奶可以退货吗？',
    'A: 鲜奶属于生鲜商品，未开封且签收 24 小时内可申请退货；已开封不支持退货。',
    '',
    'Q: 多久发货？',
    'A: 现货商品在付款后 24 小时内发出。'
  ].join('\n')
  const imported = await request.post(`${API}/admin/v1/ai/knowledge/import`, {
    headers,
    multipart: { file: { name: 'policy.md', mimeType: 'text/markdown', buffer: Buffer.from(md, 'utf-8') } }
  })
  expect(imported.status(), await imported.text()).toBe(200)

  // 1) 资料内的问题：应给出带引用的答案
  const grounded = await request.post(`${API}/admin/v1/ai/ask`, {
    headers, data: { question: '鲜奶未开封能退货吗？多久内？' }
  })
  expect(grounded.status(), await grounded.text()).toBe(200)
  const answer = await grounded.json()
  expect(answer.fallback).toBe(false)
  expect(answer.answer).toContain('24')
  expect(answer.citations.length).toBeGreaterThan(0)
  console.log('ANSWER:', answer.answer)

  // 2) 资料外的问题：必须回落转人工，不能编
  const unknown = await request.post(`${API}/admin/v1/ai/ask`, {
    headers, data: { question: '你们支持比特币支付吗？' }
  })
  const unknownAnswer = await unknown.json()
  // 资料里没有支付方式：要么检索不到（fallback），要么模型自己说明"未提及"（同样标 fallback）
  expect(unknownAnswer.fallback || /未提及|没查到|无法确认|转人工/.test(unknownAnswer.answer)).toBe(true)
})
