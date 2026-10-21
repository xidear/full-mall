import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'

test.skip(process.env.APP_EDITION !== 'enterprise', '仅企业版构建运行')

test('enterprise ai imports knowledge and answers with citations', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const headers = { Authorization: `Bearer ${login.token}` }

  const status = await (await request.get(`${API}/admin/v1/ai/status`, { headers })).json()
  expect(status.edition).toBe('enterprise')
  expect(status.ready).toBeTruthy()

  const markdown = [
    'Q: 鲜奶可以退货吗？',
    'A: 鲜奶属于生鲜商品，未开封且签收 24 小时内可申请退货。',
    '',
    'Q: 多久发货？',
    'A: 现货商品在付款后 24 小时内发出。'
  ].join('\n')

  const imported = await request.post(`${API}/admin/v1/ai/knowledge/import`, {
    headers,
    multipart: {
      file: { name: 'faq.md', mimeType: 'text/markdown',
              buffer: Buffer.from(markdown, 'utf-8') }
    }
  })
  expect(imported.status(), await imported.text()).toBe(200)
  const importBody = await imported.json()
  expect(importBody.chunks).toBe(2)

  const asked = await request.post(`${API}/admin/v1/ai/ask`, {
    headers, data: { question: '鲜奶可以退货吗' }
  })
  expect(asked.status(), await asked.text()).toBe(200)
  const answer = await asked.json()
  expect(answer.fallback).toBe(false)
  expect(answer.answer).toContain('未开封')
  expect(answer.citations.length).toBeGreaterThan(0)
  expect(answer.citations[0].title).toContain('faq.md')
})
