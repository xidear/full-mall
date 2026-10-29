import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'
import { e2ePhone } from './utils/e2ePhone'

/**
 * 国别驱动地址表单（ADR-0013 §8 / ADR-0014）真机测试。
 *
 * 断言的是「形状随国家变」：
 *   日本 = 都道府县下拉 47 项 + 7 位邮编；
 *   新加坡 = 只有地址行 + 6 位邮编，没有省市；
 *   中国 = 省下拉 34 项，选中后出现城市下拉。
 *
 * 加购走真实 UI（与 storefront.spec.ts 同一条路径）：用接口加购与页面读的购物车
 * 归属不一致，会得到"购物车为空"这种假失败。
 */
const API = 'http://localhost:41280'

async function loginCookie(page: Page, request: import('@playwright/test').APIRequestContext) {
  const phone = e2ePhone()
  const issued = await request.post(`${API}/api/v1/member/auth/otp`, { data: { phone } })
  const code = (await issued.json()).debugCode
  const token = (await (await request.post(`${API}/api/v1/member/auth/verify`,
    { data: { phone, code } })).json()).token
  await page.context().addCookies([{
    name: 'fullmall_member_token', value: token, domain: 'localhost', path: '/', sameSite: 'Lax'
  }])
  return phone
}

/** 等 Nuxt 水合完成再点按钮：未水合时点击不触发事件，会得到"点了没反应"的假失败。 */
async function waitForNuxtHydration(page: Page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('#__nuxt') as (HTMLElement & { __vue_app__?: unknown }) | null
    return Boolean(root?.__vue_app__)
  })
}

async function addToCartViaUi(page: Page) {
  await page.goto('/products?keyword=' + encodeURIComponent('有机鲜牛奶'))
  const card = page.locator('.product-card', { hasText: '有机鲜牛奶' })
  await expect(card).toBeVisible()
  await card.getByRole('link', { name: '查看详情' }).click()
  await expect(page.getByRole('heading', { name: '有机鲜牛奶' })).toBeVisible()
  await waitForNuxtHydration(page)
  await page.getByRole('button', { name: '加入购物车' }).click()
  // 购物车角标出现即视为加购成功：比盯着 toast 更稳（toast 会自动消失）
  await expect(page.locator('.cart-link')).toContainText(/\d/)
}

test('结算页地址表单随国家变化：日本有都道府县、新加坡没有省市、中国是三级', async ({ page, request }) => {
  await addToCartViaUi(page)
  const phone = await loginCookie(page, request)
  expect(phone).toMatch(/^199\d{8}$/)

  await page.goto('/checkout', { waitUntil: 'networkidle' })
  await expect(page.getByRole('heading', { name: '结算' })).toBeVisible()

  // --- 日本：都道府县是下拉（47 + 1 个"请选择"），邮编占位符取该国第一个示例 ---
  await page.locator('#addr-country').selectOption('JP')
  await expect(page.locator('#addr-state option')).toHaveCount(48)
  await expect(page.locator('#addr-postal')).toHaveAttribute('placeholder', '154-0023')

  // 邮编填错要立刻提示（正则来自后端 address_format.zip）
  await page.locator('#addr-postal').fill('12')
  await expect(page.getByText('邮政编码格式不正确')).toBeVisible()

  // --- 新加坡：只有地址行 + 邮编，没有省/州和城市 ---
  await page.locator('#addr-country').selectOption('SG')
  await expect(page.locator('#addr-state')).toHaveCount(0)
  await expect(page.locator('#addr-city')).toHaveCount(0)
  await expect(page.locator('#addr-postal')).toHaveAttribute('placeholder', '546080')
  // 门禁同源：CN 市场未开通 SG 目的地，前台必须当场提示，而不是等提交才报错
  await expect(page.getByText('暂不支持配送到该国家/地区，请更换国家或联系客服')).toBeVisible()

  // --- 中国：省下拉 34 项 + 1 个"请选择"；选省后出现城市下拉 ---
  await page.locator('#addr-country').selectOption('CN')
  await expect(page.locator('#addr-state option')).toHaveCount(35)
  await expect(page.locator('#addr-city')).toBeVisible()
})

test('用中国地址完成一次配送下单（地址国家随订单落库）', async ({ page, request }) => {
  await addToCartViaUi(page)
  const phone = await loginCookie(page, request)
  await page.goto('/checkout', { waitUntil: 'networkidle' })

  await page.locator('#addr-country').selectOption('CN')
  // 省级列表是异步加载的：先等选项出现（此时才从文本输入切成下拉），再选
  await expect(page.locator('#addr-state option')).toHaveCount(35)
  await page.locator('#addr-state').selectOption({ label: '北京市' })
  // 北京市是直辖市：下一级直接是区（16 个 + 1 个"请选择"）。
  // 用 toHaveCount 而不是可见性：select 收起时 option 会被判定为 hidden，那会误报失败。
  await expect(page.locator('#addr-city option')).toHaveCount(17)
  await page.locator('#addr-detail').fill('朝阳区望京街道 1 号')
  await page.locator('#addr-postal').fill('100102')

  await page.locator('#contact-name').fill('地址校验顾客')
  await page.locator('#contact-phone').fill(phone)
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: /提交订单|Mock 支付并下单/ }).click()

  await page.waitForURL(/\/orders\//, { timeout: 30_000 })
})
