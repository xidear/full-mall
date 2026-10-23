import { expect, test } from '@playwright/test'

async function waitForNuxtHydration(page: import('@playwright/test').Page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('#__nuxt') as (HTMLElement & { __vue_app__?: unknown }) | null
    return Boolean(root?.__vue_app__)
  })
}

/**
 * 砍价页 E2E：真实建一个活动（走后台接口），再打开前台页验证展示与未登录拦截。
 * 不依赖种子数据。
 */
test('bargain page renders real activity and blocks anonymous start', async ({ page, request }) => {
  const captcha = await request.get('http://localhost:8080/admin/v1/auth/captcha')
  expect(captcha.ok()).toBeTruthy()
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
  expect(login.ok(), `管理员登录失败：${login.status()}`).toBeTruthy()

  const token = (await login.json()).token as string
  const products = await request.get('http://localhost:8080/admin/v1/products?size=1', {
    headers: { Authorization: `Bearer ${token}` }
  })
  expect(products.ok(), `读取商品列表失败：${products.status()}`).toBeTruthy()
  const body = await products.json()
  const skuId = body.items?.[0]?.skus?.[0]?.id ?? body.items?.[0]?.skuId
  expect(skuId, '商品列表里没有可用 SKU').toBeTruthy()

  const now = Date.now()
  const created = await request.post('http://localhost:8080/admin/v1/bargain-activities', {
    headers: { Authorization: `Bearer ${token}` },
    data: {
      skuId,
      floorPrice: 100,
      maxCut: 5000,
      minHelpers: 3,
      cutMin: 50,
      cutMax: 100,
      expiresHours: 24,
      allowBelowCost: true,
      belowCostReason: 'E2E 低价验证',
      startsAt: new Date(now - 3600_000).toISOString(),
      endsAt: new Date(now + 86_400_000).toISOString()
    }
  })
  expect(created.ok(), `创建活动失败：${created.status()} ${await created.text()}`).toBeTruthy()
  const activityId = (await created.json()).activityId as string
  expect(activityId).toBeTruthy()

  await page.goto(`/bargain/${activityId}`)
  await waitForNuxtHydration(page)

  await expect(page.getByRole('heading', { level: 1 })).toBeVisible()
  await expect(page.getByText(/原价 ¥/)).toBeVisible()
  await expect(page.getByText(/最低可到 ¥1\.00/)).toBeVisible()

  await expect(page.getByText('未登录：可查看活动，发起与帮砍需先登录。')).toBeVisible()
  // 水合完成后按钮才有事件处理器，否则点击无反应
  await page.waitForTimeout(400)
  await page.getByRole('button', { name: '发起砍价' }).click()
  await expect(page.getByText('请先登录后再发起砍价')).toBeVisible()
})
