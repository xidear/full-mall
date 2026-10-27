import { expect, test, type APIRequestContext, type Page, type Response } from '@playwright/test'

/**
 * Full Mall Storefront 全站浏览器审计（真实 Chromium + DOM/可访问性选择器）
 *
 * 运行：
 *   cd web
 *   npx playwright test --config=audit/playwright.audit.config.ts
 *   npx playwright test --config=audit/playwright.audit.config.ts --grep "语言"
 *
 * 说明：
 * 1) 仓库根配置 web/playwright.config.ts 的 testDir 是 ./e2e，直接传 audit/ 路径会 "No tests found"，
 *    因此使用同目录下的 playwright.audit.config.ts（不改动原配置）。
 * 2) 默认基址 http://localhost:41285（唯一同时满足安全上下文与后端 CORS 白名单的来源）。
 *    可用 STOREFRONT_BASE_URL / STOREFRONT_LOOPBACK_URL / STOREFRONT_LAN_URL 覆盖。
 * 3) 所有断言均为「设计意图 vs 实际」的硬断言（expect.soft 只用于一次性收集同页多个缺陷，
 *    测试结束时仍会失败）；不使用 try/catch 兜底，不用截图识图代替断言。
 * 4) 结算用例会向演示库写入 1 笔演示订单（与现有 e2e 一致），不改代码/表结构。
 */

const BASE_URL = process.env.STOREFRONT_BASE_URL ?? 'http://localhost:41285'
const LOOPBACK_URL = process.env.STOREFRONT_LOOPBACK_URL ?? 'http://127.0.0.1:41285'
const LAN_URL = process.env.STOREFRONT_LAN_URL ?? 'http://192.168.1.15:41285'

// 演示数据：有机鲜牛奶（单规格、批次效期、含秒杀 + 买赠活动）
const PRODUCT_ID = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
const PRODUCT_TITLE = '有机鲜牛奶'

test.use({ baseURL: BASE_URL })

interface Recorder {
  consoleErrors: string[]
  consoleWarnings: string[]
  pageErrors: string[]
  failedRequests: string[]
  httpErrors: string[]
}

function record(page: Page): Recorder {
  const rec: Recorder = {
    consoleErrors: [],
    consoleWarnings: [],
    pageErrors: [],
    failedRequests: [],
    httpErrors: []
  }
  page.on('console', (message) => {
    const text = message.text()
    if (message.type() === 'error') rec.consoleErrors.push(text)
    if (message.type() === 'warning') rec.consoleWarnings.push(text)
  })
  page.on('pageerror', (error) => rec.pageErrors.push(error.message))
  page.on('requestfailed', (request) => {
    rec.failedRequests.push(`${request.method()} ${request.url()} ${request.failure()?.errorText ?? ''}`.trim())
  })
  page.on('response', (response) => {
    if (response.status() >= 400) rec.httpErrors.push(`${response.status()} ${response.url()}`)
  })
  return rec
}

const hydrationWarnings = (rec: Recorder) =>
  rec.consoleWarnings.filter((warning) => /hydration/i.test(warning))

async function open(page: Page, path: string): Promise<Response | null> {
  return page.goto(path, { waitUntil: 'networkidle' })
}

async function waitForHydration(page: Page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('#__nuxt') as (HTMLElement & { __vue_app__?: unknown }) | null
    return Boolean(root?.__vue_app__)
  })
}

async function addCurrentProductToCart(page: Page) {
  await open(page, `/product/${PRODUCT_ID}`)
  await waitForHydration(page)
  await page.getByRole('button', { name: '加入购物车' }).click()
}

/* ------------------------------------------------------------------ */
/* 1. 首页                                                             */
/* ------------------------------------------------------------------ */
test('首页：首屏结构、分类入口、商品卡与主 CTA', async ({ page }) => {
  const rec = record(page)
  const response = await open(page, '/')
  expect.soft(response?.status(), '首页应返回 200').toBe(200)

  await expect.soft(page).toHaveTitle(/Full Mall/)
  await expect.soft(page.getByRole('heading', { level: 1 })).toContainText('好物直达')
  await expect.soft(page.locator('.category-tile')).toHaveCount(6)
  await expect.soft(page.locator('.product-card').first()).toBeVisible()
  await expect.soft(page.getByRole('heading', { name: '今日特惠' })).toBeVisible()
  await expect.soft(page.getByRole('heading', { name: '大家都在买' })).toBeVisible()
  await expect.soft(page.locator('.hero-actions').getByRole('link')).toHaveCount(2)

  expect.soft(rec.pageErrors, '首页加载不应有未捕获异常').toEqual([])

  // 主 CTA 必须真的发生导航（“部分链接点击无响应”回归点）
  const before = page.url()
  await page.getByRole('link', { name: '立即选购' }).click()
  await expect.soft(page).not.toHaveURL(before)
  await expect.soft(page).toHaveURL(/\/products$/)
})

/* ------------------------------------------------------------------ */
/* 2. 顶部导航 / 分类导航                                              */
/* ------------------------------------------------------------------ */
test('顶部导航：分类与“全部商品”必须打开可用的商品列表', async ({ page }) => {
  await open(page, '/')
  await waitForHydration(page)

  await page.locator('.category-nav').getByRole('link', { name: '全部商品' }).click()
  await expect.soft(page).toHaveURL(/\/products/)

  // 实际：/products SSR 500（categories is not defined），标题仍是首页 h1 或 500 页
  await expect.soft(page.getByRole('heading', { level: 1, name: '全部商品' })).toBeVisible()
  await expect.soft(page.locator('.product-grid .product-card').first()).toBeVisible()
})

/* ------------------------------------------------------------------ */
/* 3. 商品列表：筛选 / 排序 / 搜索 / 分页 / 空态                       */
/* ------------------------------------------------------------------ */
test('商品列表：筛选、排序、搜索、分页、空态', async ({ page }) => {
  const response = await open(page, '/products')
  const status = response?.status()
  expect.soft(status, '/products 应返回 200（实际 SSR 500）').toBe(200)

  if (status !== 200) {
    test.info().annotations.push({
      type: 'blocked',
      description: '/products 返回 500，筛选/排序/分页/空态无法在真实页面上验证；以下断言仅用于记录页面内容'
    })
    await expect.soft(page.locator('body')).not.toContainText('categories is not defined')
    return
  }

  await expect.soft(page.getByRole('heading', { level: 1, name: '全部商品' })).toBeVisible()
  await expect.soft(page.locator('.filter-sidebar .filter-link')).toHaveCount(7)
  await expect.soft(page.locator('.product-card')).toHaveCount(11)

  // 价格排序按钮
  const sortButton = page.locator('.sort-select')
  await expect.soft(sortButton).toHaveText(/价格/)
  await sortButton.click()
  await expect.soft(sortButton).toHaveText(/↑/)

  // 分类筛选
  await page.locator('.filter-sidebar').getByRole('link', { name: '生鲜食品' }).click()
  await expect.soft(page).toHaveURL(/categoryId=/)
  await expect.soft(page.getByRole('heading', { level: 1 })).toHaveText('生鲜食品')

  // 搜索与空态
  await open(page, `/products?keyword=${encodeURIComponent('牛奶')}`)
  await expect.soft(page.getByRole('heading', { level: 1 })).toContainText('搜索：牛奶')
  await expect.soft(page.locator('.product-card').first()).toContainText('牛奶')

  await open(page, `/products?keyword=${encodeURIComponent('绝对不存在的商品xyz')}`)
  await expect.soft(page.locator('.empty-state')).toContainText('没有找到符合条件的商品')
  await expect.soft(page.locator('.empty-state').getByRole('link')).toHaveCount(1)

  await open(page, '/products')
  await expect.soft(page.locator('.empty-state')).toContainText('已经到底了')
})

/* ------------------------------------------------------------------ */
/* 4. 顶部搜索                                                         */
/* ------------------------------------------------------------------ */
test('搜索：输入关键字后必须给出可访问的搜索结果', async ({ page }) => {
  await open(page, '/')
  await waitForHydration(page)

  const input = page.locator('.search-box input')
  await expect.soft(input).toHaveAccessibleName(/搜索|search/i)

  await input.fill('牛奶')
  await page.locator('.search-box button').click()
  await expect.soft(page).toHaveURL(/\/products\?keyword=/)
  await expect.soft(page.getByRole('heading', { level: 1 })).toContainText('搜索：牛奶')
  await expect.soft(page.locator('.product-card').first()).toContainText('牛奶')
})

/* ------------------------------------------------------------------ */
/* 5. 商品详情：规格 / 加购 / 收藏 / 评价                              */
/* ------------------------------------------------------------------ */
test('商品详情：主图、规格、加购、收藏、评价', async ({ page }) => {
  const rec = record(page)
  await open(page, `/product/${PRODUCT_ID}`)
  await waitForHydration(page)

  await expect.soft(page.getByRole('heading', { level: 1 })).toHaveText(PRODUCT_TITLE)
  await expect.soft(page.locator('.price-large')).toBeVisible()

  // 主图必须真实加载（实际 /assets/assets/... 404）
  const heroLoaded = await page.locator('.product-detail-media img').evaluate((img: HTMLImageElement) => img.naturalWidth > 0)
  expect.soft(heroLoaded, '商品详情主图应成功加载（naturalWidth > 0）').toBe(true)

  // 规格：演示数据中没有多规格商品，规格切换 UI 不渲染
  const chips = page.locator('.option-chip')
  const chipCount = await chips.count()
  expect.soft(chipCount, '商品详情应有规格选择（演示数据无多规格 SKU）').toBeGreaterThan(0)

  // 加购
  await page.getByRole('button', { name: '加入购物车' }).click()
  await expect.soft(page.locator('.success-text')).toHaveText('已加入购物车')
  await expect.soft(page.locator('.cart-count')).toHaveText('1')

  // 收藏 / 评价：设计意图是详情页提供入口
  await expect.soft(page.getByRole('button', { name: /收藏/ }), '商品详情页应有收藏按钮').toHaveCount(1)
  await expect.soft(page.getByRole('heading', { name: /评价|评价晒单/ }), '商品详情页应有评价区').toHaveCount(1)

  // 面包屑可点击
  await expect.soft(page.locator('.breadcrumb').getByRole('link', { name: '首页' })).toHaveAttribute('href', '/')
  await expect.soft(page.locator('.breadcrumb').getByRole('link', { name: '全部商品' })).toHaveAttribute('href', '/products')

  expect.soft(rec.pageErrors, '商品详情页不应有未捕获异常').toEqual([])
})

/* ------------------------------------------------------------------ */
/* 6. 购物车                                                           */
/* ------------------------------------------------------------------ */
test('购物车：改数量、删除、结算入口', async ({ page }) => {
  await addCurrentProductToCart(page)
  await page.locator('.cart-link').click()
  await expect.soft(page.getByRole('heading', { level: 1 })).toHaveText('我的购物车')
  await expect.soft(page.locator('.cart-line')).toHaveCount(1)

  // 商品缩略图应为真实图片（实际只有写死的 “FM” 占位）
  await expect.soft(page.locator('.cart-thumb img'), '购物车行应有商品缩略图').toHaveCount(1)

  await page.getByRole('button', { name: '＋' }).click()
  await expect.soft(page.locator('.cart-quantity span')).toHaveText('2')
  await page.getByRole('button', { name: '−' }).click()
  await expect.soft(page.locator('.cart-quantity span')).toHaveText('1')

  await page.getByRole('button', { name: '删除' }).click()
  await expect.soft(page.getByRole('heading', { name: '购物车还是空的' })).toBeVisible()
  await expect.soft(page.getByRole('link', { name: '去逛逛' })).toHaveAttribute('href', '/products')
})

/* ------------------------------------------------------------------ */
/* 7. 结算 / 下单 / 订单详情 / 我的订单                                */
/* ------------------------------------------------------------------ */
test('结算与下单：地址、验证码、条款、Mock 支付、订单详情、订单列表', async ({ page }) => {
  const rec = record(page)

  // 空购物车提交应有明确提示
  await open(page, '/checkout')
  await page.locator('.summary-panel button.button').click()
  await expect.soft(page.locator('.error-text')).toContainText('购物车为空')

  await addCurrentProductToCart(page)
  await open(page, '/checkout')
  await waitForHydration(page)

  await expect.soft(page.locator('.summary-panel button.button')).toContainText(/Mock 支付并下单|查看演示结果/)
  await expect.soft(page.locator('.summary-list')).toContainText(PRODUCT_TITLE)

  // 门店自提：门店下拉应有数据
  await page.getByRole('button', { name: '门店自提' }).click()
  await expect.soft(page.locator('.field select option').first()).toBeAttached()
  const storeCount = await page.locator('.field select option').count()
  expect.soft(storeCount, '门店自提应至少返回 1 个门店').toBeGreaterThan(0)

  // 获取验证码：不能把技术异常直接抛给顾客
  await page.getByRole('button', { name: '获取验证码' }).click()
  await expect.soft(page.locator('text=演示环境自动填入')).toBeVisible({ timeout: 10_000 })
  await expect.soft(page.locator('.error-text'), '点击“获取验证码”不应出现错误').toHaveCount(0)

  await page.getByRole('checkbox').check()
  await page.locator('.summary-panel button.button').click()

  await expect.soft(page).toHaveURL(/\/orders\/[0-9a-f-]+/, { timeout: 25_000 })
  if (!/\/orders\//.test(page.url())) {
    test.info().annotations.push({ type: 'blocked', description: '下单未跳转订单页，订单详情/订单列表无法验证' })
    return
  }

  await expect.soft(page.getByRole('heading', { level: 1 })).toContainText('订单 FM')
  await expect.soft(page.locator('.status-badge').first()).toContainText(/已付款|待发货|已支付/)
  await expect.soft(page.getByRole('heading', { name: '履约' })).toBeVisible()
  await expect.soft(page.getByRole('heading', { name: '申请售后' })).toBeVisible()

  // 下单后“我的订单”必须能列出该订单（实际因缺少鉴权头 404）
  await open(page, '/account')
  await page.waitForTimeout(1500)
  const orderCards = await page.locator('.order-card').count()
  expect.soft(orderCards, '下单后 /account 应至少列出 1 个订单').toBeGreaterThan(0)
  await expect.soft(page.locator('.account-page')).not.toContainText('404')

  expect.soft(rec.pageErrors, '结算链路不应有未捕获异常').toEqual([])
})

/* ------------------------------------------------------------------ */
/* 8. 促销 / 优惠券 / 砍价                                             */
/* ------------------------------------------------------------------ */
test('促销：秒杀买赠拼团可交互，并提供领券与砍价入口', async ({ page }) => {
  await open(page, '/promotions')
  await waitForHydration(page)

  await expect.soft(page.locator('.promotion-card')).toHaveCount(3)
  await expect.soft(page.locator('.promotion-card.flash')).toHaveCount(1)
  await expect.soft(page.locator('.promotion-card.gift')).toHaveCount(1)
  await expect.soft(page.locator('.promotion-card.group')).toHaveCount(1)

  // 拼团弹窗：能打开、能关闭、空提交有提示
  await page.locator('.promotion-card.group').getByRole('button').click()
  await expect.soft(page.locator('.promotion-modal')).toBeVisible()
  await page.getByRole('button', { name: '加入拼团', exact: true }).click()
  await expect.soft(page.locator('.promotion-modal .error-text')).toContainText('请输入团号')
  await page.getByRole('button', { name: '关闭' }).click()
  await expect.soft(page.locator('.promotion-modal')).toHaveCount(0)

  // 领券入口（设计要求：促销页应能领券）
  const couponEntry = page.getByRole('link', { name: /领券|优惠券/ })
  await expect.soft(couponEntry, '促销页应有“领券”入口').not.toHaveCount(0)

  // 砍价入口（bargain 页存在但全站无入口）
  const bargainEntry = page.locator('a[href*="/bargain"]')
  await expect.soft(bargainEntry, '促销页应有“砍价”入口').not.toHaveCount(0)

  // 秒杀按钮应直达结算
  await page.locator('.promotion-card.flash').getByRole('button').click()
  await expect.soft(page).toHaveURL(/\/checkout/)
})

/* ------------------------------------------------------------------ */
/* 9. 法律页面                                                         */
/* ------------------------------------------------------------------ */
test('法律页：TERMS / PRIVACY / PICKUP 必须可读', async ({ page }) => {
  const expected: Array<[string, string]> = [
    ['TERMS', '用户服务协议'],
    ['PRIVACY', '隐私政策'],
    ['PICKUP', '门店自提']
  ]
  for (const [type, title] of expected) {
    const response = await open(page, `/legal/${type}`)
    expect.soft(response?.status(), `/legal/${type} 应返回 200`).toBe(200)
    await expect.soft(page).not.toHaveTitle(/404/)
    await expect.soft(page.getByRole('heading', { level: 1 }), `/legal/${type} 标题`).toContainText(title)
    await expect.soft(page.locator('.legal-content')).toBeVisible()
  }
})

/* ------------------------------------------------------------------ */
/* 10. 页脚与中国合规信息                                              */
/* ------------------------------------------------------------------ */
test('页脚：ICP/公安备案/营业执照/客服电话与邮箱必须可点击', async ({ page }) => {
  await open(page, '/')

  const footer = page.locator('.site-footer')
  await expect.soft(footer).toBeVisible()
  await expect.soft(footer).toContainText('京ICP备00000000号-1')
  await expect.soft(footer).toContainText('演示商城（中国）有限公司')

  await expect.soft(page.locator('.site-footer a[href*="beian.miit.gov.cn"]'), 'ICP 备案号应链接到 beian.miit.gov.cn').toHaveCount(1)
  await expect.soft(page.locator('.site-footer a[href*="beian.mps.gov.cn"], .site-footer a[href*="mps.gov.cn"]'), '公安备案号应有链接').toHaveCount(1)
  await expect.soft(page.locator('.site-footer a', { hasText: /营业执照|经营许可证/ }), '应有营业执照/经营许可证入口').toHaveCount(1)
  await expect.soft(page.locator('.site-footer a[href^="tel:"]'), '客服电话应为 tel: 链接').toHaveCount(1)
  await expect.soft(page.locator('.site-footer a[href^="mailto:"]'), '客服邮箱应为 mailto: 链接').toHaveCount(1)

  const footerLinks = await page.locator('.site-footer a').count()
  expect.soft(footerLinks, '页脚应包含合规与客服链接').toBeGreaterThanOrEqual(4)
})

/* ------------------------------------------------------------------ */
/* 11. 语言切换                                                        */
/* ------------------------------------------------------------------ */
test('语言切换：应为下拉菜单，且影响文案、html lang 与刷新后持久化', async ({ page }) => {
  await open(page, '/')
  await waitForHydration(page)

  const switcher = page.locator('.locale-switcher')
  await expect.soft(switcher).toBeVisible()

  // 设计要求：语言多时应为下拉菜单，而不是把按钮铺满
  await expect.soft(
    switcher.locator('select, [role="combobox"], [aria-haspopup]'),
    '语言切换应为下拉菜单（当前是平铺按钮）'
  ).toHaveCount(1)

  const before = await page.locator('.site-header').innerText()
  // 兼容两种实现：下拉（select）或平铺按钮；两者都必须能切到 en
  const localeSelect = switcher.locator('select')
  if (await localeSelect.count()) {
    await localeSelect.selectOption('en')
  } else {
    await switcher.getByRole('button', { name: 'English' }).click()
  }
  await page.waitForTimeout(2500)

  const after = await page.locator('.site-header').innerText()
  expect.soft(after, '切换英文后页面文案应变化').not.toBe(before)
  await expect.soft(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect.soft(page.locator('.cart-link')).toContainText('Cart')

  await page.reload({ waitUntil: 'networkidle' })
  const activeSelect = page.locator('.locale-switcher select')
  if (await activeSelect.count()) {
    await expect.soft(activeSelect, '刷新后语言下拉应保持 English').toHaveValue('en')
  } else {
    await expect.soft(page.locator('.locale-switcher .active')).toHaveText('English')
  }
  await expect.soft(page.locator('html')).toHaveAttribute('lang', 'en')
  await expect.soft(page.getByRole('link', { name: '购物车' })).toHaveCount(0)
})

/* ------------------------------------------------------------------ */
/* 12. 全站链接审计（href 有效性 + 点击后可达）                        */
/* ------------------------------------------------------------------ */
test('链接审计：无空 href / # / javascript:，内部链接均可打开', async ({ page, request }) => {
  const paths = [
    '/',
    '/cart',
    '/checkout',
    `/product/${PRODUCT_ID}`,
    '/promotions',
    '/account',
    '/favorites',
    '/history',
    '/notifications',
    '/legal/TERMS',
    '/nope-404'
  ]

  const deadHrefs: string[] = []
  const internalHrefs = new Set<string>()

  for (const path of paths) {
    await open(page, path)
    const anchors = await page.$$eval('a', (nodes) =>
      nodes.map((node) => ({
        text: (node.textContent ?? '').trim().replace(/\s+/g, ' ').slice(0, 30),
        href: node.getAttribute('href'),
        absolute: (node as HTMLAnchorElement).href
      }))
    )
    for (const anchor of anchors) {
      const href = anchor.href ?? ''
      if (href === '' || href === '#' || href.startsWith('javascript:')) {
        deadHrefs.push(`${path} → “${anchor.text}” href=${JSON.stringify(href)}`)
      }
      if (anchor.absolute.startsWith(BASE_URL)) {
        internalHrefs.add(new URL(anchor.absolute).pathname + new URL(anchor.absolute).search)
      }
    }
  }

  expect.soft(deadHrefs, '存在空 href / # / javascript: 链接').toEqual([])

  const broken = await checkLinks(request, [...internalHrefs])
  expect.soft(broken, '内部链接存在 4xx/5xx 或软 404').toEqual([])
})

async function checkLinks(request: APIRequestContext, hrefs: string[]): Promise<string[]> {
  const broken: string[] = []
  for (const href of hrefs.slice(0, 200)) {
    // /nope-404 是 404 用例的固定探针，不属于站点内部可达链接
    if (href === '/nope-404') continue
    const response = await request.get(href)
    const body = await response.text()
    const soft404 = body.includes('404 - ') && body.includes('| Nuxt')
    if (response.status() >= 400 || soft404) {
      broken.push(`${href} → HTTP ${response.status()}${soft404 ? '（软 404 页面）' : ''}`)
    }
  }
  return broken
}

/* ------------------------------------------------------------------ */
/* 13. 控制台 / 网络 / hydration                                       */
/* ------------------------------------------------------------------ */
test('运行时质量：控制台无报错、无 hydration mismatch、静态资源 200', async ({ page }) => {
  const paths = ['/', `/product/${PRODUCT_ID}`, '/promotions', '/cart', '/checkout']
  const report: string[] = []

  for (const path of paths) {
    const rec = record(page)
    await open(page, path)
    await page.waitForTimeout(1000)
    if (rec.consoleErrors.length) report.push(`${path} console.error: ${rec.consoleErrors.join(' | ').slice(0, 300)}`)
    if (hydrationWarnings(rec).length) report.push(`${path} hydration: ${hydrationWarnings(rec).length} 条`)
    if (rec.pageErrors.length) report.push(`${path} pageerror: ${rec.pageErrors.join(' | ').slice(0, 300)}`)
    const asset404 = rec.httpErrors.filter((entry) => entry.includes('/assets/'))
    if (asset404.length) report.push(`${path} 资源 404: ${asset404.slice(0, 3).join(' | ')}`)
    const apiFailures = rec.failedRequests.filter((entry) => entry.includes('/api/'))
    if (apiFailures.length) report.push(`${path} 请求失败: ${apiFailures.join(' | ')}`)
  }

  expect.soft(report, '页面存在控制台报错 / hydration mismatch / 资源失败').toEqual([])
})

/* ------------------------------------------------------------------ */
/* 14. 移动端 390px                                                    */
/* ------------------------------------------------------------------ */
test.describe('移动端 390px 视口', () => {
  test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })

  test('移动端：无横向溢出、触控目标 >=44px、导航可用', async ({ page }) => {
    await open(page, '/')
    await waitForHydration(page)

    const overflow = await page.evaluate(() => ({
      doc: document.documentElement.scrollWidth,
      win: window.innerWidth
    }))
    expect.soft(overflow.doc, '移动端不应出现整页横向滚动').toBeLessThanOrEqual(overflow.win + 1)

    const smallTargets = await page.evaluate(() => {
      const result: Array<{ text: string; width: number; height: number }> = []
      document.querySelectorAll<HTMLElement>('.topbar a, .category-nav a, .search-box button, .locale-switcher button').forEach((el) => {
        const rect = el.getBoundingClientRect()
        if (rect.width === 0 || rect.height === 0) return
        if (rect.height < 44 || rect.width < 44) {
          result.push({
            text: (el.textContent ?? '').trim().slice(0, 12),
            width: Math.round(rect.width),
            height: Math.round(rect.height)
          })
        }
      })
      return result
    })
    expect.soft(smallTargets, '主要导航/操作触控目标应 >= 44x44').toEqual([])

    // 分类导航 8 项：要么全部可见，要么有折叠菜单
    const navMetrics = await page.evaluate(() => {
      const nav = document.querySelector('.category-nav') as HTMLElement | null
      return nav ? { scrollWidth: nav.scrollWidth, clientWidth: nav.clientWidth } : { scrollWidth: 0, clientWidth: 0 }
    })
    const hasMenuToggle = await page.locator('[aria-label*="菜单"], .menu-toggle, .hamburger').count()
    expect.soft(
      hasMenuToggle > 0 || navMetrics.scrollWidth <= navMetrics.clientWidth + 1,
      `移动端分类导航需横向滚动（${navMetrics.scrollWidth}px / 可视 ${navMetrics.clientWidth}px）且无折叠菜单`
    ).toBe(true)

    // 商品详情页移动端主操作区
    await open(page, `/product/${PRODUCT_ID}`)
    await expect.soft(page.getByRole('button', { name: '加入购物车' })).toBeVisible()
    await expect.soft(page.getByRole('button', { name: '立即购买' })).toBeVisible()
  })
})

/* ------------------------------------------------------------------ */
/* 15. 404 页面                                                        */
/* ------------------------------------------------------------------ */
test('404 页：状态码、中文文案、返回入口与站点框架', async ({ page }) => {
  const response = await open(page, '/nope-404')
  expect.soft(response?.status(), '未知路径应返回 404').toBe(404)
  await expect.soft(page.getByRole('heading', { level: 1 })).toContainText('404')
  await expect.soft(page.getByRole('link', { name: /返回首页|回到首页|Go back home/ })).toHaveAttribute('href', '/')
  await expect.soft(page, '中文站点 404 应为中文文案').not.toHaveTitle(/Page not found/)
  await expect.soft(page.locator('.site-header'), '404 页应保留站点导航').toBeVisible()
})

/* ------------------------------------------------------------------ */
/* 16. 访问来源一致性（localhost / 127.0.0.1 / 局域网 IP）             */
/* ------------------------------------------------------------------ */
test('来源一致性：三个访问地址都应能加购且无 CORS 报错', async ({ browser }) => {
  for (const origin of [BASE_URL, LOOPBACK_URL, LAN_URL]) {
    const context = await browser.newContext({ baseURL: origin })
    const page = await context.newPage()
    const rec = record(page)

    await open(page, `/product/${PRODUCT_ID}`)
    await page.getByRole('button', { name: '加入购物车' }).click()

    await expect.soft(page.locator('.success-text'), `${origin} 加购应有成功提示`).toBeVisible({ timeout: 4000 })
    const cart = await page.evaluate(() => localStorage.getItem('fullmall.cart'))
    expect.soft(cart, `${origin} 加购后应写入购物车`).not.toBeNull()

    const cors = [...rec.consoleErrors, ...rec.pageErrors].filter((entry) => /CORS|Failed to fetch|randomUUID/i.test(entry))
    expect.soft(cors, `${origin} 不应出现 CORS / 安全上下文错误`).toEqual([])

    await context.close()
  }
})
