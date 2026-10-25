import { expect, test } from '@playwright/test'

async function login(page: import('@playwright/test').Page) {
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()
}

test('admin creates a full-reduction rule through the form', async ({ page }) => {
  await login(page)

  // 清理同名历史规则，保证断言唯一
  const code = `E2E-RULE-${Date.now().toString().slice(-6)}`
  await page.locator('.admin-menu').getByText('促销管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '促销管理' })).toBeVisible()

  await page.getByRole('button', { name: '新建规则' }).click()
  const dialog = page.getByRole('dialog', { name: '新建满减/满折规则' })
  await dialog.getByPlaceholder('例如 FULL-200-20').fill(code)
  await dialog.getByPlaceholder('例如 满 200 减 20').fill('E2E 满减规则')
  // 门槛/减免/类型/范围/渠道/时间均用默认值
  await dialog.getByRole('button', { name: '创建' }).click()

  await expect(page.getByText('规则已创建（草稿），启用后生效')).toBeVisible()

  // 界面列表出现该规则
  const row = page.locator('.el-table__row', { hasText: code })
  await expect(row).toBeVisible()
  await expect(row.getByText('草稿')).toBeVisible()

  // 启用后状态变为进行中
  await row.getByRole('button', { name: '启用' }).click()
  await expect(page.getByText('规则已启用')).toBeVisible({ timeout: 5000 })
  await expect(page.locator('.el-table__row', { hasText: code }).getByText('进行中'))
    .toBeVisible({ timeout: 5000 })
})
