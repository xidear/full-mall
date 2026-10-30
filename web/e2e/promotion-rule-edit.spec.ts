import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('admin edits a draft promotion rule', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('促销管理', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '促销管理', level: 2 })).toBeVisible()

  // 新建一条草稿规则（唯一编码）
  const code = `E2E-EDIT-${Date.now().toString().slice(-6)}`
  await page.getByRole('button', { name: '新建规则' }).click()
  const dialog = page.getByRole('dialog', { name: '新建满减/满折规则' })
  await dialog.getByPlaceholder('例如 FULL-200-20').fill(code)
  await dialog.getByPlaceholder('例如 满 200 减 20').fill('编辑前名称')
  await dialog.getByRole('button', { name: '创建' }).click()
  await expect(page.getByText('规则已创建（草稿），启用后生效')).toBeVisible()

  const row = page.locator('.el-table__row', { hasText: code })
  await expect(row).toBeVisible()

  // 编辑：改名称
  await row.getByRole('button', { name: '编辑' }).click()
  const editDialog = page.getByRole('dialog', { name: '编辑满减/满折规则' })
  const titleInput = editDialog.getByPlaceholder('例如 满 200 减 20')
  await titleInput.fill('编辑后名称')
  await editDialog.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('规则已更新')).toBeVisible({ timeout: 10_000 })

  // 列表反映修改（重新加载后仍是新名称）
  await expect(page.locator('.el-table__row', { hasText: code })).toContainText('编辑后名称')
})
