import { expect, test } from '@playwright/test'

/** 素材库内一键生成变体：选参考图 → 提交 → worker 生成 → 结果回显 → 挂到商品。 */
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
    multipart: { file: { name: 'ref.png', mimeType: 'image/png', buffer: png } }
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

  // worker 每 10s 消费一次，给足时间
  await expect(page.locator('.job-state .el-tag')).toHaveText(/SUCCEEDED|FAILED/, { timeout: 90_000 })
  await expect(page.locator('.job-state')).toContainText('SUCCEEDED')
  await expect(page.locator('.job-state .result')).toBeVisible()

  await expect(page.getByRole('button', { name: '挂载' })).toBeVisible()
})
