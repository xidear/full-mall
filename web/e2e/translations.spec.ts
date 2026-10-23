import { expect, test } from '@playwright/test'

/**
 * 译文链路 E2E：admin 录入 en 标题 → 审核 → 发布 → 前台按 locale 读到英文。
 * 走真实接口，不 mock。
 */
test('admin publishes english title and storefront serves it', async ({ page, request }) => {
  const captcha = await request.get('http://localhost:8080/admin/v1/auth/captcha')
  const challenge = await captcha.json() as { captchaId: string; question: string }
  const numbers = (challenge.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post('http://localhost:8080/admin/v1/auth/login', {
    data: {
      username: 'admin',
      password: 'Admin@123456',
      captchaId: challenge.captchaId,
      captchaAnswer: String(numbers[0] + numbers[1])
    }
  })
  expect(login.ok()).toBeTruthy()
  const token = (await login.json()).token as string
  const auth = { Authorization: `Bearer ${token}` }

  const products = await request.get('http://localhost:8080/admin/v1/products?page=0&size=1', { headers: auth })
  const body = await products.json()
  const product = body.items?.[0]
  expect(product?.id, '没有商品可用于译文测试').toBeTruthy()

  const englishTitle = `Organic Milk EN ${Date.now()}`

  const saved = await request.put(
    `http://localhost:8080/admin/v1/products/${product.id}/translations/en`,
    { headers: auth, data: { title: englishTitle, description: 'Fresh organic milk' } })
  expect(saved.ok(), `保存译文失败：${saved.status()} ${await saved.text()}`).toBeTruthy()

  const reviewed = await request.post(
    `http://localhost:8080/admin/v1/products/${product.id}/translations/en/review`, { headers: auth, data: {} })
  expect(reviewed.ok(), `审核失败：${reviewed.status()}`).toBeTruthy()

  const published = await request.post(
    `http://localhost:8080/admin/v1/products/${product.id}/translations/en/publish`, { headers: auth, data: {} })
  expect(published.ok(), `发布失败：${published.status()} ${await published.text()}`).toBeTruthy()

  const en = await request.get(
    `http://localhost:8080/api/v1/products/${product.id}?locale=en`)
  expect(en.ok()).toBeTruthy()
  expect(await en.text()).toContain(englishTitle)

  const zh = await request.get(
    `http://localhost:8080/api/v1/products/${product.id}?locale=zh-CN`)
  expect(await zh.text()).not.toContain(englishTitle)

  // 后台页需要登录态：注入 token 后再访问
  await page.addInitScript((value) => {
    localStorage.setItem('fullmall.admin.token', value)
  }, token)
  await page.goto('http://localhost:5273/translations')
  await expect(page.locator('h1', { hasText: '商品译文' }).first()).toBeVisible()
})
