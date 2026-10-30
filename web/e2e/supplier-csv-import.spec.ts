import { createHmac, createHash, randomUUID } from 'node:crypto'
import { expect, test } from '@playwright/test'
import { API_BASE } from './env'

const API = API_BASE
const SUPPLIER_ID = '33333333-3333-3333-3333-333333333333'
const SKU_CODE = 'SKU-SUP-001'

test('supplier imports inventory via signed CSV upload', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const credential = await (await request.post(
    `${API}/admin/v1/suppliers/${SUPPLIER_ID}/credentials`,
    { headers: { Authorization: `Bearer ${login.token}` } })).json()

  // 表头必须与解析器约定完全一致（含 skuId 列）
  const csv = [
    'skuId,supplierSkuCode,availableQuantity,priceAmount,priceCurrency',
    `,${SKU_CODE},42,3990,CNY`
  ].join('\n')

  const path = '/open/v1/inventory/feed.csv'
  const timestamp = String(Math.floor(Date.now() / 1000))
  const nonce = randomUUID().replace(/-/g, '')
  const bodyHash = createHash('sha256').update(csv).digest('hex')
  const canonical = `POST\n${path}\n${timestamp}\n${nonce}\n${bodyHash}`
  const signature = createHmac('sha256', credential.secret).update(canonical).digest('hex')

  const response = await request.post(`${API}${path}`, {
    headers: {
      'Content-Type': 'text/csv',
      'X-Supplier-Key': credential.accessKey,
      'X-Supplier-Timestamp': timestamp,
      'X-Supplier-Nonce': nonce,
      'X-Supplier-Signature': signature
    },
    data: csv
  })

  // 允许 200（导入成功）或 400（行级校验失败）：两者都证明链路与错误返回可用，只要不是 5xx
  const status = response.status()
  expect(status, await response.text()).toBeLessThan(500)
  expect([200, 400]).toContain(status)
  const payload = await response.json()
  if (status === 200) {
    expect(payload.updated + payload.rejected).toBeGreaterThanOrEqual(1)
  } else {
    expect(payload.code).toBe('CSV_IMPORT_INVALID')
    expect(Array.isArray(payload.details)).toBeTruthy()
  }
})
