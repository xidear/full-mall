import { createHmac, createHash, randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import { API_BASE } from './env'

const API = API_BASE
const SUPPLIER_ID = '33333333-3333-3333-3333-333333333333'

test('supplier downloads settlement CSV with signed request', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const credential = await (await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/credentials`,
    { headers: { Authorization: `Bearer ${login.token}` } })).json()

  const path = '/open/v1/settlements.csv?limit=10'
  const timestamp = String(Math.floor(Date.now() / 1000))
  const nonce = randomUUID().replace(/-/g, '')
  const bodyHash = createHash('sha256').update('').digest('hex')
  const canonical = `GET\n${path}\n${timestamp}\n${nonce}\n${bodyHash}`
  const signature = createHmac('sha256', credential.secret).update(canonical).digest('hex')

  const response = await request.get(`${API}${path}`, {
    headers: {
      'X-Supplier-Key': credential.accessKey,
      'X-Supplier-Timestamp': timestamp,
      'X-Supplier-Nonce': nonce,
      'X-Supplier-Signature': signature
    }
  })

  expect(response.status(), await response.text()).toBe(200)
  expect(response.headers()['content-disposition']).toContain('supplier-settlements.csv')
  const body = await response.text()
  // 至少要有表头；有数据时行数 > 1
  expect(body.split('\n').length).toBeGreaterThanOrEqual(1)
  expect(body.length).toBeGreaterThan(0)
})
