import { expect, test } from '@playwright/test'
import { API_BASE } from './env'

const API = API_BASE

test('admin creates a member export job and downloads the csv', async ({ request }) => {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await (await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456',
            captchaId: captcha.captchaId, captchaAnswer: operands[0] + operands[1] }
  })).json()
  const headers = { Authorization: `Bearer ${login.token}` }

  const created = await request.post(`${API}/admin/v1/export-jobs`, {
    headers,
    data: { resource: 'member', projection: 'default', params: {} }
  })
  expect(created.status(), await created.text()).toBe(202)
  const jobId = (await created.json()).jobId as string
  expect(jobId).toBeTruthy()

  // 轮询直到 SUCCEEDED / FAILED（异步 worker）
  let status = 'PENDING'
  for (let i = 0; i < 40 && status !== 'SUCCEEDED' && status !== 'FAILED'; i += 1) {
    await new Promise(resolve => setTimeout(resolve, 250))
    const job = await (await request.get(`${API}/admin/v1/export-jobs/${jobId}`, { headers })).json()
    status = job.status
  }
  expect(status, '导出任务应完成（不允许一直 PENDING）').toBe('SUCCEEDED')

  const download = await request.get(`${API}/admin/v1/export-jobs/${jobId}/download`, { headers })
  expect(download.status(), await download.text()).toBe(200)
  const csv = await download.text()
  expect(csv.split('\n').length).toBeGreaterThanOrEqual(1)
})
