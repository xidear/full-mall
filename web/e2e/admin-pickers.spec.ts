import { expect, test } from '@playwright/test'
import { ADMIN_BASE, API_BASE } from './env'

/** 资源引用全部用选择器：商品编辑选规格图、AI 页选商品、分镜选商品。 */
// 守住的回归：后台资源引用一律走选择器（商品、规格图），不再手填 UUID。
// 依赖：41280/41281 已起、演示商品 aaaaaaaa-… 存在、素材上传可用（purpose=PRODUCT 才进素材库）。
// 失败线索：弹窗打不开先看素材是否上传成功；下面那条 placeholder 断言就是“别改回手填”的守门人。
test('admin picks assets and products instead of typing ids', async ({ page, request }) => {
  const captcha = await (await request.get(API_BASE + '/admin/v1/auth/captcha')).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(API_BASE + '/admin/v1/auth/login', {
    data: { username: 'admin', password: 'Admin@123456', captchaId: captcha.captchaId, captchaAnswer: String(nums[0] + nums[1]) }
  })
  const token = (await login.json()).token as string

  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64')
  const uploaded = await request.post(API_BASE + '/admin/v1/media?purpose=PRODUCT', {
    headers: { Authorization: `Bearer ${token}` },
    multipart: { file: { name: `e2e-picker-${Date.now()}.png`, mimeType: 'image/png', buffer: png } }
  })
  expect(uploaded.ok(), await uploaded.text()).toBeTruthy()
  const uploadedAssetId = (await uploaded.json()).assetId as string
  expect(uploadedAssetId, '上传应返回 assetId，否则清理会漏').toBeTruthy()

  await page.addInitScript((value) => localStorage.setItem('fullmall.admin.token', value), token)
  // 直接注入 token 跳过登录 UI：本用例只关心选择器，登录流程由 admin.spec.ts 守。
  await page.goto(ADMIN_BASE + '/products')
  await expect(page.locator('h1, h2').first()).toBeVisible()

  // 商品编辑：规格图通过素材选择器
  await page.goto(ADMIN_BASE + '/products/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/edit')
  // 编辑页已按「基础信息 / 素材 / 规格 / 译文」分标签；规格图在「规格」标签内
  await page.getByRole('tab', { name: '规格' }).click()
  await expect(page.locator('[aria-label="选择规格图"]').first()).toBeVisible({ timeout: 20_000 })
  await page.locator('[aria-label="选择规格图"]').first().click()
  await expect(page.getByRole('dialog')).toBeVisible()
  await page.locator('.grid .cell').first().click()
  await page.getByRole('button', { name: /确定（已选 1）/ }).click()
  await expect(page.locator('.asset-picker .preview').first()).toBeVisible()

  // AI 素材页：商品选择器已从表单顶部移到「生成成功后 → 挂到商品」（ProductPicker 组件），
  // 需要真实生成才能出现，e2e 不跑生成。这里守住原来的意图：**页面上不允许手填商品 ID**。
  // 选择器本身的正确性由上面的规格图选择器与 ProductPicker 的单元/组件层保证。
  await page.goto(ADMIN_BASE + '/ai-media')
  await expect(page.getByText('AI 素材生成', { exact: true }).first()).toBeVisible()
  await expect(page.getByPlaceholder(/商品\s*ID/i)).toHaveCount(0)
  await expect(page.locator('input[placeholder*="aaaaaaaa-"]')).toHaveCount(0)

  // 视频分镜：已并入「AI 素材生成」的第二个标签（旧地址 /ai-storyboard 会重定向到这里）
  await page.goto(ADMIN_BASE + '/ai-media?tab=storyboard')
  await expect(page.getByRole('tab', { name: '视频分镜' })).toHaveClass(/is-active/)
  // 商品选择器（ProductPicker）只在「生成成功后 → 挂到商品」渲染，e2e 不跑真实生成，因此这里
  // 不做正向可见断言；回归线改为负向：页面上不允许出现手填商品 ID / UUID 的输入框。
  await expect(page.getByPlaceholder(/商品ID|UUID/)).toHaveCount(0)
  await expect(page.locator('input[placeholder*="aaaaaaaa-"]')).toHaveCount(0)
  // 手填 UUID 的输入框必须彻底消失——这条计数断言就是“不再手填”的回归线。

  // 测试自清理：上传的素材必须删掉，否则演示库会被 picker.png/m.png 这类垃圾图填满
  const removed = await request.delete(`http://localhost:41280/admin/v1/assets/${uploadedAssetId}`, {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(removed.ok(), await removed.text()).toBeTruthy()
})
