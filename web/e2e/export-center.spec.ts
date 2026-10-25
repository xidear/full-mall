import { expect, test } from '@playwright/test'

test('admin generates sku_cost export from export center', async ({ page }) => {
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const nums = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(nums[0] + nums[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('导出中心', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: '导出中心' })).toBeVisible()

  // 选择"SKU 成本（ERP）"资源并生成
  await page.locator('.el-select').click()
  await page.getByRole('option', { name: 'SKU 成本（ERP）' }).click()
  await page.getByRole('button', { name: '生成导出' }).click()

  // 轮询到 SUCCEEDED（导出处理任务默认开启）
  await expect(page.getByRole('cell', { name: 'SUCCEEDED' }))
    .toBeVisible({ timeout: 30_000 })
  const downloadButton = page.getByRole('button', { name: '下载' }).first()
  await expect(downloadButton).toBeEnabled()
})
