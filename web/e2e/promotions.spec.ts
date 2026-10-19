import { expect, test } from '@playwright/test'

async function waitForNuxtHydration(page: import('@playwright/test').Page) {
  await page.waitForFunction(() => {
    const root = document.querySelector('#__nuxt') as (HTMLElement & { __vue_app__?: unknown }) | null
    return Boolean(root?.__vue_app__)
  })
}

test('storefront lists promotions and applies flash sale to checkout', async ({ page }) => {
  await page.goto('/promotions')
  await waitForNuxtHydration(page)
  await expect(page.getByRole('heading', { name: '秒杀 · 买赠 · 拼团' })).toBeVisible()
  await expect(page.getByText('马克杯限时秒杀')).toBeVisible()
  await expect(page.getByText('鲜奶买二赠一')).toBeVisible()
  await expect(page.getByText('马克杯 2 人拼团')).toBeVisible()

  const flashCard = page.locator('.promotion-card', { hasText: '马克杯限时秒杀' })
  await flashCard.getByRole('button', { name: '立即抢购' }).click()
  await expect(page.getByRole('heading', { name: '结算' })).toBeVisible()
  await expect(page.getByText('手工陶瓷马克杯').first()).toBeVisible()
})

test('product page shows promotion badges and group buy dialog', async ({ page }) => {
  await page.goto('/product/cccccccc-cccc-cccc-cccc-cccccccccccc')
  await waitForNuxtHydration(page)
  await expect(page.getByText(/秒杀 .* 剩余/)).toBeVisible()
  await expect(page.getByText(/2 人拼团价/)).toBeVisible()

  await page.goto('/product/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa')
  await waitForNuxtHydration(page)
  await expect(page.getByText(/买 2 赠 1/)).toBeVisible()

  await page.goto('/promotions')
  await waitForNuxtHydration(page)
  const groupCard = page.locator('.promotion-card', { hasText: '马克杯 2 人拼团' })
  await groupCard.getByRole('button', { name: '发起 / 加入拼团' }).click()
  const groupModal = page.locator('.promotion-modal')
  await expect(groupModal.getByRole('heading', { name: '马克杯 2 人拼团' })).toBeVisible()
  await expect(groupModal.getByPlaceholder('用于参团与限购校验')).toBeVisible()
  await expect(groupModal.getByPlaceholder('例如 GBABCD1234')).toBeVisible()
})
