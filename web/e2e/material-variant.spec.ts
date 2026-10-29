import { expect, test } from '@playwright/test'

/** 素材库内一键生成变体：选参考图 → 提交 → worker 生成 → 结果回显 → 挂到商品。 */
// 守住的回归：素材库“选参考图 → 生成变体 → worker 消费 → 结果回显 → 可挂载”整条链路不断。
// 依赖：41280/41281、素材上传（purpose=PRODUCT）、变体 worker（每 10s 消费一次）与 AI mock（41288）。
// 失败线索：90s 仍无标签 → 先看 worker 在不在跑、AI provider 是否指向 mock；失败原因看 .job-state 文案。
test('material library generates a variant end to end', async ({ page, request }) => {
  const captcha = await (await request.get('http://localhost:41280/admin/v1/auth/captcha')).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post('http://localhost:41280/admin/v1/auth/login', {
    data: { username: 'admin', password: 'Admin@123456', captchaId: captcha.captchaId, captchaAnswer: String(nums[0] + nums[1]) }
  })
  expect(login.ok()).toBeTruthy()
  const token = (await login.json()).token as string

  // 造一张参考图（直接走上传接口，purpose=PRODUCT 才会进素材库）
  const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64')
  const uploaded = await request.post('http://localhost:41280/admin/v1/media?purpose=PRODUCT', {
    headers: { Authorization: `Bearer ${token}` },
    multipart: { file: { name: `e2e-ref-${Date.now()}.png`, mimeType: 'image/png', buffer: png } }
  })
  expect(uploaded.ok(), await uploaded.text()).toBeTruthy()

  await page.addInitScript((value) => {
    localStorage.setItem('fullmall.admin.token', value)
  }, token)
  await page.goto('http://localhost:41281/materials')
  await expect(page.locator('main h1, .page h1, h1').filter({ hasText: '素材库' }).first()).toBeVisible()

  await page.getByRole('button', { name: 'AI 生成变体' }).click()
  await expect(page.getByRole('dialog')).toBeVisible()

  // 选第一张参考图 + 填提示词
  await page.locator('.variant-grid .thumb').first().click()
  await page.locator('.variant-grid textarea').first().fill('白底商品主图，45 度侧光')
  await page.getByRole('button', { name: '生成', exact: true }).click()

  // worker 每 10s 消费一次，给足时间；UI 显示的是本地化文案（已完成为 SUCCEEDED 的中文标签）
  // 90s 超时是因为 worker 10s 轮询一次；先断言“已完成或失败”再断言“已完成”，
  // 这样异步失败时能得到可读原因，而不是一条干巴巴的超时。
  await expect(page.locator('.job-state .el-tag')).toHaveText(/已完成|失败/, { timeout: 90_000 })
  await expect(page.locator('.job-state')).toContainText('已完成')
  await expect(page.locator('.job-state .result')).toBeVisible()

  await expect(page.getByRole('button', { name: '挂载' })).toBeVisible()

  // 测试自清理：参考图与生成结果都要删掉（否则每跑一次素材库多两张垃圾图）
  const uploadedAssetId = (await uploaded.json()).assetId as string
  await request.delete(`http://localhost:41280/admin/v1/assets/${uploadedAssetId}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  const listed = await request.get('http://localhost:41280/admin/v1/assets/search?page=0&size=5', {
    headers: { Authorization: `Bearer ${token}` }
  })
  const page1 = await listed.json() as { items: Array<{ id: string; name: string }> }
  const generated = page1.items.find(item => item.name.startsWith('AI '))
  if (generated) {
    const removed = await request.delete(`http://localhost:41280/admin/v1/assets/${generated.id}`, {
      headers: { Authorization: `Bearer ${token}` }
    })
    expect(removed.ok(), await removed.text()).toBeTruthy()
  }
})
