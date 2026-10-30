import { expect, test } from '@playwright/test'
import { e2ePhone } from './utils/e2ePhone'
import { API_BASE, STOREFRONT_HOST } from './env'

async function waitForNuxtHydration(page: import('@playwright/test').Page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('#__nuxt') as (HTMLElement & { __vue_app__?: unknown }) | null
    return Boolean(root?.__vue_app__)
  })
}

test('storefront supports configured theme preview', async ({ page }) => {
  await page.goto('/?theme=industrial')
  await expect(page.locator('.app-shell')).toHaveAttribute('data-theme', 'industrial')
  await page.goto('/?theme=mall')
  await expect(page.locator('.app-shell')).toHaveAttribute('data-theme', 'mall')
})

test('storefront can browse and create a pickup order', async ({ page, request }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /好物直达/ })).toBeVisible()

  // 首页只显示最新 N 件；演示库商品变多后「有机鲜牛奶」未必在第一屏。
  // 走「全部商品 + 关键词」这条路，既覆盖搜索，也不再依赖首页排序。
  await page.goto('/products?keyword=' + encodeURIComponent('有机鲜牛奶'))
  const milkCard = page.locator('.product-card', { hasText: '有机鲜牛奶' })
  await expect(milkCard).toBeVisible()
  await milkCard.getByRole('link', { name: '查看详情' }).click()

  await expect(page.getByRole('heading', { name: '有机鲜牛奶' })).toBeVisible()
  await waitForNuxtHydration(page)
  await page.getByRole('button', { name: '加入购物车' }).click()
  await expect(page.getByText('已加入购物车')).toBeVisible()
  await page.locator('.cart-link').click()
  await expect(page.getByRole('heading', { name: '我的购物车' })).toBeVisible()
  await page.getByRole('link', { name: '去结算' }).click()

  await expect(page.getByRole('heading', { name: '结算' })).toBeVisible()
  // 结算现在要求先登录（页面给「登录后继续结算」入口），访客 OTP 表单已移除。
  // 这里直接用 API 拿会员 token 写 cookie，等价于已登录用户，覆盖的是"登录后可结算"这条真实路径。
  const phone = e2ePhone()
  const issued = await request.post(API_BASE + '/api/v1/member/auth/otp', { data: { phone } })
  const code = (await issued.json()).debugCode
  const member = (await (await request.post(API_BASE + '/api/v1/member/auth/verify',
    { data: { phone, code } })).json()).token
  await page.context().addCookies([{
    name: 'fullmall_member_token', value: member, domain: STOREFRONT_HOST, path: '/', sameSite: 'Lax'
  }])
  await page.reload({ waitUntil: 'networkidle' })
  await expect(page.getByRole('heading', { name: '结算' })).toBeVisible()

  await page.getByRole('button', { name: '门店自提' }).click()
  await page.locator('.field select').selectOption({ index: 0 })
  // 登录用户结算是「提交订单」；演示模式另有一个 mock 支付按钮，两种都接受
  await page.locator('#contact-name').fill('E2E 顾客')
  // 手机号必须是已登录会员自己的号码，否则页面会要求补短信验证码
  await page.locator('#contact-phone').fill(phone)
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: /提交订单|Mock 支付并下单/ }).click()

  await page.waitForURL(/\/orders\//, { timeout: 30_000 })
  await expect(page.getByText(/订单 FM/)).toBeVisible()
  await expect(page.getByText('已付款')).toBeVisible()
})

test('cart page can change quantity and remove item', async ({ page }) => {
  await page.goto('/product/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
  await waitForNuxtHydration(page)
  await page.getByRole('button', { name: '加入购物车' }).click()
  await expect(page.getByText('已加入购物车')).toBeVisible()
  await page.locator('.cart-link').click()
  await expect(page.getByRole('heading', { name: '我的购物车' })).toBeVisible()
  await expect(page.getByText('有机鲜牛奶').first()).toBeVisible()
  await page.getByRole('button', { name: '＋' }).click()
  await expect(page.locator('.cart-quantity span')).toHaveText('2')
  await page.getByRole('button', { name: '删除' }).click()
  await expect(page.getByRole('heading', { name: '购物车还是空的' })).toBeVisible()
})