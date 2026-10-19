import { expect, test } from '@playwright/test'

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

test('storefront can browse and create a pickup order', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByRole('heading', { name: /好物直达/ })).toBeVisible()

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
  await page.getByRole('button', { name: '门店自提' }).click()
  await page.locator('.field select').selectOption({ index: 0 })
  await page.getByRole('checkbox').check()
  await page.getByRole('button', { name: 'Mock 支付并下单' }).click()

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