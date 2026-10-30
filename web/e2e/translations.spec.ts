import { expect, test } from '@playwright/test'
import { ADMIN_BASE, API_BASE } from './env'

/**
 * 译文链路 E2E：admin 录入 en 标题 → 审核 → 发布 → 前台按 locale 读到英文。
 * 走真实接口，不 mock。
 */
// 守住的回归：旧 /translations 入口仍能跳转（第二个用例）+ 译文不会串到默认语言。
// 依赖：41280/41281 已起、至少 1 个商品、admin / Admin@123456 可用。
test('admin publishes english title and storefront serves it', async ({ page, request }) => {
  const captcha = await request.get(API_BASE + '/admin/v1/auth/captcha')
  const challenge = await captcha.json() as { captchaId: string; question: string }
  const numbers = (challenge.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(API_BASE + '/admin/v1/auth/login', {
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

  const products = await request.get(API_BASE + '/admin/v1/products?page=0&size=1', { headers: auth })
  const body = await products.json()
  const product = body.items?.[0]
  expect(product?.id, '没有商品可用于译文测试').toBeTruthy()

  const englishTitle = `Organic Milk EN ${Date.now()}`
  // 英文标题带 Date.now()：重复运行时才不会读到上一次留下的旧译文而假通过。

  const saved = await request.put(
    `http://localhost:41280/admin/v1/products/${product.id}/translations/en`,
    { headers: auth, data: { title: englishTitle, description: 'Fresh organic milk' } })
  expect(saved.ok(), `保存译文失败：${saved.status()} ${await saved.text()}`).toBeTruthy()

  const reviewed = await request.post(
    `http://localhost:41280/admin/v1/products/${product.id}/translations/en/review`, { headers: auth, data: {} })
  expect(reviewed.ok(), `审核失败：${reviewed.status()}`).toBeTruthy()

  const published = await request.post(
    `http://localhost:41280/admin/v1/products/${product.id}/translations/en/publish`, { headers: auth, data: {} })
  expect(published.ok(), `发布失败：${published.status()} ${await published.text()}`).toBeTruthy()

  const en = await request.get(
    `http://localhost:41280/api/v1/products/${product.id}?locale=en`)
  expect(en.ok()).toBeTruthy()
  expect(await en.text()).toContain(englishTitle)

  // 同时断言 zh-CN 不含英文标题：否则“译文覆盖了默认语言”这种串味缺陷发现不了。
  const zh = await request.get(
    `http://localhost:41280/api/v1/products/${product.id}?locale=zh-CN`)
  expect(await zh.text()).not.toContain(englishTitle)


  // 后台页需要登录态：注入 token 后再访问
  await page.addInitScript((value) => {
    localStorage.setItem('fullmall.admin.token', value)
  }, token)
  // 译文已并入商品管理：直接打开该商品的「译文」标签页
  await page.goto(ADMIN_BASE + '/products/' + product.id + '/edit?tab=translations')
  await expect(page.getByRole('tab', { name: 'English' }).first()).toBeVisible()
  await expect(page.locator('h3', { hasText: 'English（en）' }).first()).toBeVisible()
  await expect(page.getByRole('button', { name: 'AI 翻译' }).first()).toBeVisible()
})

// 老书签和文档里的 /translations 必须仍可达；只改导航等于把入口藏起来，这里守住重定向。
test('legacy /translations link redirects into product management', async ({ page, request }) => {
  const captcha = await request.get(API_BASE + '/admin/v1/auth/captcha')
  const challenge = await captcha.json() as { captchaId: string; question: string }
  const numbers = (challenge.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(API_BASE + '/admin/v1/auth/login', {
    data: {
      username: 'admin',
      password: 'Admin@123456',
      captchaId: challenge.captchaId,
      captchaAnswer: String(numbers[0] + numbers[1])
    }
  })
  const token = (await login.json()).token as string
  await page.addInitScript((value) => {
    localStorage.setItem('fullmall.admin.token', value)
  }, token)
  await page.goto(ADMIN_BASE + '/translations')
  await expect(page.locator('h2', { hasText: '商品管理' }).first()).toBeVisible()
})
