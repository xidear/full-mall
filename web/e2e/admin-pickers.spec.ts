import { expect, test } from '@playwright/test'

/** 资源引用全部用选择器：商品编辑选规格图、AI 页选商品、分镜选商品。 */
test('admin picks assets and products instead of typing ids', async ({ page, request }) => {
  const captcha = await (await request.get('http://localhost:8080/admin/v1/auth/captcha')).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post('http://localhost:8080/admin/v1/auth/login', {
    data: { username: 'admin', password: 'Admin@123456', captchaId: captcha.captchaId, captchaAnswer: String(nums[0] + nums[1]) }
  })
  const token = (await login.json()).token as string

  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64')
  const uploaded = await request.post('http://localhost:8080/admin/v1/media?purpose=PRODUCT', {
    headers: { Authorization: `Bearer ${token}` },
    multipart: { file: { name: 'picker.png', mimeType: 'image/png', buffer: png } }
  })
  expect(uploaded.ok(), await uploaded.text()).toBeTruthy()

  await page.addInitScript((value) => localStorage.setItem('fullmall.admin.token', value), token)
  await page.goto('http://localhost:5273/products')
  await expect(page.locator('h1, h2').first()).toBeVisible()

  // 商品编辑：规格图通过素材选择器
  await page.goto('http://localhost:5273/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/edit')
  await expect(page.locator('[aria-label="选择规格图"]').first()).toBeVisible({ timeout: 20_000 })
  await page.locator('[aria-label="选择规格图"]').first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.locator('.grid .cell').first().click()
  await page.getByRole('button', { name: /确定（已选 1）/ }).click()
  await expect(page.locator('.asset-picker .preview').first()).toBeVisible()

  // AI 素材页：商品用远程搜索选择器
  await page.goto('http://localhost:5273/ai-media')
  await expect(page.getByText('AI 素材生成', { exact: true }).first()).toBeVisible()
  const productSelect = page.locator('.el-form-item', { hasText: '商品' }).locator('.el-select').first()
  await productSelect.click()
  await expect(page.locator('.el-select-dropdown__item').first()).toBeVisible({ timeout: 10_000 })
  await page.locator('.el-select-dropdown__item').first().click()

  // 视频分镜页：商品选择器存在（不再手填 UUID）
  await page.goto('http://localhost:5273/ai-storyboard')
  await expect(page.locator('.el-form-item', { hasText: '商品' }).locator('.el-select').first()).toBeVisible()
  await expect(page.getByPlaceholder(/商品ID|UUID/)).toHaveCount(0)
})
