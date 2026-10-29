import { expect, test } from '@playwright/test'

/** 资源引用全部用选择器：商品编辑选规格图、AI 页选商品、分镜选商品。 */
// 守住的回归：后台资源引用一律走选择器（商品、规格图），不再手填 UUID。
// 依赖：41280/41281 已起、演示商品 aaaaaaaa-… 存在、素材上传可用（purpose=PRODUCT 才进素材库）。
// 失败线索：弹窗打不开先看素材是否上传成功；下面那条 placeholder 断言就是“别改回手填”的守门人。
test('admin picks assets and products instead of typing ids', async ({ page, request }) => {
  const captcha = await (await request.get('http://localhost:41280/admin/v1/auth/captcha')).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post('http://localhost:41280/admin/v1/auth/login', {
    data: { username: 'admin', password: 'Admin@123456', captchaId: captcha.captchaId, captchaAnswer: String(nums[0] + nums[1]) }
  })
  const token = (await login.json()).token as string

  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64')
  const uploaded = await request.post('http://localhost:41280/admin/v1/media?purpose=PRODUCT', {
    headers: { Authorization: `Bearer ${token}` },
    multipart: { file: { name: `e2e-picker-${Date.now()}.png`, mimeType: 'image/png', buffer: png } }
  })
  expect(uploaded.ok(), await uploaded.text()).toBeTruthy()
  const uploadedAssetId = (await uploaded.json()).assetId as string
  expect(uploadedAssetId, '上传应返回 assetId，否则清理会漏').toBeTruthy()

  await page.addInitScript((value) => localStorage.setItem('fullmall.admin.token', value), token)
  // 直接注入 token 跳过登录 UI：本用例只关心选择器，登录流程由 admin.spec.ts 守。
  await page.goto('http://localhost:41281/products')
  await expect(page.locator('h1, h2').first()).toBeVisible()

  // 商品编辑：规格图通过素材选择器
  await page.goto('http://localhost:41281/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/edit')
  // 编辑页已按「基础信息 / 素材 / 规格 / 译文」分标签；规格图在「规格」标签内
  await page.getByRole('tab', { name: '规格' }).click()
  await expect(page.locator('[aria-label="选择规格图"]').first()).toBeVisible({ timeout: 20_000 })
  await page.locator('[aria-label="选择规格图"]').first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.locator('.grid .cell').first().click()
  await page.getByRole('button', { name: /确定（已选 1）/ }).click()
  await expect(page.locator('.asset-picker .preview').first()).toBeVisible()

  // AI 素材页：商品用远程搜索选择器
  await page.goto('http://localhost:41281/ai-media')
  await expect(page.getByText('AI 素材生成', { exact: true }).first()).toBeVisible()
  const productSelect = page.locator('.el-form-item', { hasText: '商品' }).locator('.el-select').first()
  await productSelect.click()
  await expect(page.locator('.el-select-dropdown__item').first()).toBeVisible({ timeout: 10_000 })
  await page.locator('.el-select-dropdown__item').first().click()

  // 视频分镜页：商品选择器存在（不再手填 UUID）
  await page.goto('http://localhost:41281/ai-storyboard')
  await expect(page.locator('.el-form-item', { hasText: '商品' }).locator('.el-select').first()).toBeVisible()
  await expect(page.getByPlaceholder(/商品ID|UUID/)).toHaveCount(0)
  // 手填 UUID 的输入框必须彻底消失——这条计数断言就是“不再手填”的回归线。

  // 测试自清理：上传的素材必须删掉，否则演示库会被 picker.png/m.png 这类垃圾图填满
  const removed = await request.delete(`http://localhost:41280/admin/v1/assets/${uploadedAssetId}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(removed.ok(), await removed.text()).toBeTruthy()
})
