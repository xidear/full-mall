import { expect, test } from '@playwright/test'

const API = 'http://localhost:41280'
const PRODUCT = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'
const PNG = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
  'base64')

async function adminToken(request: import('@playwright/test').APIRequestContext) {
  const captcha = await (await request.get(`${API}/admin/v1/auth/captcha`)).json()
  const nums = (captcha.question.match(/\d+/g) ?? []).map(Number)
  const login = await request.post(`${API}/admin/v1/auth/login`, {
    data: { username: 'admin', password: 'Admin@123456', captchaId: captcha.captchaId, captchaAnswer: String(nums[0] + nums[1]) }
  })
  expect(login.ok(), await login.text()).toBeTruthy()
  return (await login.json()).token as string
}

/** 用户端商品页真实展示：主图 / 图集 / 详情图（来自素材库）。 */
test('storefront product page shows main, gallery and detail media', async ({ page, request }) => {
  const token = await adminToken(request)
  const auth = { Authorization: `Bearer ${token}` }

  const urls: string[] = []
  for (const purpose of ['PRODUCT', 'PRODUCT', 'PRODUCT']) {
    const uploaded = await request.post(`${API}/admin/v1/media?purpose=${purpose}&source=E2E`, {
      headers: auth,
      multipart: { file: { name: 'm.png', mimeType: 'image/png', buffer: PNG } }
    })
    expect(uploaded.ok(), await uploaded.text()).toBeTruthy()
    urls.push((await uploaded.json()).assetId as string)
  }

  // 幂等：先清掉该商品既有素材，避免重复运行撞唯一索引
  const existing = await (await request.get(
    `${API}/admin/v1/products/${PRODUCT}/media`, { headers: auth })).json()
  for (const row of existing) {
    await request.delete(`${API}/admin/v1/products/${PRODUCT}/media/${row.id}`, { headers: auth })
  }

  const attach = async (assetId: string, role: string, sortOrder: number) => {
    const res = await request.post(`${API}/admin/v1/products/${PRODUCT}/media`, {
      headers: auth, data: { assetId, role, sortOrder, source: 'AI' }
    })
    expect(res.ok(), await res.text()).toBeTruthy()
  }
  await attach(urls[0], 'MAIN', 0)
  await attach(urls[1], 'GALLERY', 1)
  await attach(urls[2], 'DETAIL', 0)

  await page.goto(`http://localhost:41285/product/${PRODUCT}`)
  await page.waitForFunction(() => {
    const root = document.querySelector('#__nuxt') as (HTMLElement & { __vue_app__?: unknown }) | null
    return Boolean(root?.__vue_app__)
  })

  await expect(page.locator('.product-detail-media img')).toBeVisible()
  await expect(page.locator('.product-media-strip img')).toHaveCount(1)
  await expect(page.locator('.product-detail-images img')).toHaveCount(1)
})
