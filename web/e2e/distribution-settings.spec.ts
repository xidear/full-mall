import { expect, test } from '@playwright/test'

test('admin edits distribution settings and they persist', async ({ page }) => {
  await page.goto('http://localhost:5273/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('系统设置', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '分销设置' })).toBeVisible()

  // 一级佣金值改成 7（百分比）
  const level1Field = page.locator('.el-form-item', { hasText: '一级佣金值' })
  await level1Field.locator('input').fill('7')
  // 最低提现改成 200 元
  const minWithdrawal = page.getByLabel('最低提现（元）')
  await minWithdrawal.fill('200')

  await page.getByRole('button', { name: '保存设置' }).click()
  await expect(page.getByText('设置已保存')).toBeVisible({ timeout: 10_000 })

  // 重新加载页面，值必须还在（证明落库，不是只改了内存）
  await page.reload()
  await expect(page.getByRole('heading', { name: '分销设置' })).toBeVisible()
  await expect(page.locator('.el-form-item', { hasText: '一级佣金值' })
    .locator('input')).toHaveValue('7')
  await expect(page.getByLabel('最低提现（元）')).toHaveValue('200.00')
})
