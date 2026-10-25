import { expect, test } from '@playwright/test'

test('admin completes first-run setup wizard', async ({ page }) => {
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.goto('http://localhost:41281/setup')
  await expect(page.getByRole('heading', { name: '首次配置', level: 2 })).toBeVisible()

  // 第 1 步：站点名称
  await page.getByPlaceholder('例如 Full Mall').fill('E2E 商城')
  await page.getByRole('button', { name: '保存并下一步' }).click()
  await expect(page.getByText('积分过期规则')).toBeVisible()

  // 第 2 步：保存默认运营规则
  await page.getByRole('button', { name: '保存并下一步' }).click()
  await expect(page.getByText('支付服务商')).toBeVisible()

  // 第 3 步：保存服务商（保持 mock），进入完成页
  await page.getByRole('button', { name: '保存并下一步' }).click()
  await expect(page.getByText('配置已就绪')).toBeVisible()

  await page.getByRole('button', { name: '完成并进入后台' }).click()
  await expect(page.getByText('首次配置已完成')).toBeVisible({ timeout: 10_000 })
})
