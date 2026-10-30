import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

test('audit page exposes entity snapshot tab and queries history', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const nums = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(nums[0] + nums[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('操作审计', { exact: true }).click()
  await page.getByRole('tab', { name: '实体快照' }).click()

  // 允许查询的类型（asset）+ 一个不存在的实体：应返回空列表而不是类型错误
  await page.getByPlaceholder('实体类型，如 asset').fill('asset')
  await page.getByPlaceholder('实体 ID（UUID）').fill(crypto.randomUUID())
  await page.getByRole('tabpanel', { name: '实体快照' }).getByRole('button', { name: '查询' }).click()

  await expect(page.getByText('暂无快照')).toBeVisible({ timeout: 10_000 })

  // 非法类型必须给出明确错误，而不是静默空列表
  await page.getByPlaceholder('实体类型，如 asset').fill('not-a-type')
  await page.getByRole('tabpanel', { name: '实体快照' }).getByRole('button', { name: '查询' }).click()
  await expect(page.getByText(/不支持查询该类型的审计记录/)).toBeVisible({ timeout: 10_000 })
})
