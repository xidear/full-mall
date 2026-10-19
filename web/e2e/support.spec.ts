import { expect, test } from '@playwright/test'

const API = 'http://localhost:8080'
const OPERATOR = 'aaaaaaaa-0000-0000-0000-000000000002'
const ADMIN = 'aaaaaaaa-0000-0000-0000-000000000001'

test('support agent claims conversation, sends product and transfers', async ({ page, request }) => {
  const subject = `E2E 客服会话 ${Date.now()}`
  const conversationResponse = await request.post(`${API}/api/v1/support/conversations`, {
    data: {
      subject,
      locale: 'zh-CN',
      customerName: 'E2E 顾客',
      customerPhone: '13800000005'
    }
  })
  expect(conversationResponse.status()).toBe(201)
  const conversation = await conversationResponse.json()
  const claimResponse = await request.post(
    `${API}/support/v1/conversations/${conversation.id}/claim`,
    { data: { agentId: OPERATOR } }
  )
  expect(claimResponse.ok()).toBeTruthy()

  await page.goto('http://localhost:5276')
  await page.locator('.agent-switch select').selectOption(OPERATOR)
  const item = page.locator('.conversation-item', { hasText: subject })
  await expect(item).toBeVisible()
  await item.click()

  await page.getByPlaceholder('输入消息，回车发送').fill('您好，请问有什么可以帮您？')
  await page.getByRole('button', { name: '发送', exact: true }).click()
  await expect(page.locator('.bubble', { hasText: '您好，请问有什么可以帮您？' })).toBeVisible()

  await page.getByRole('button', { name: '商品', exact: true }).click()
  await page.locator('.modal').getByRole('button', { name: '发送', exact: true }).click()
  await expect(page.locator('.product-card')).toBeVisible()
  await expect(page.locator('.product-card').getByText('打开商品并锁定推广关系')).toBeVisible()

  await page.getByRole('button', { name: '转单', exact: true }).click()
  await page.locator('.modal select').selectOption(ADMIN)
  await page.locator('.modal input').fill('升级处理')
  await page.getByRole('button', { name: '确认转单' }).click()
  await expect(page.getByText('会话已转交')).toBeVisible()
})
