import { expect, test } from '@playwright/test'
import type { Page } from '@playwright/test'

/**
 * 批次 D/E 的 admin 页面真机测试：运费档位与税率规则。
 *
 * 断言"能建、能存、能看见"，并且断言关键口径文案存在——
 * 页面写错口径（例如把"价内含税"写成"价外"）比没有页面更危险。
 */
async function loginAdmin(page: Page) {
  await page.goto('http://localhost:41281/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()
}

test('运费档位页可以新增重量阶梯并落库', async ({ page }) => {
  await loginAdmin(page)
  await page.locator('.admin-menu').getByText('运费档位', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: '运费档位' })).toBeVisible()

  await page.getByRole('button', { name: '新增档位' }).click()
  await page.getByPlaceholder('ISO 3166-1，留空=其它目的地默认档').fill('SG')
  // 数字输入顺序：重量下限(0)、重量上限、运费、满额免运费；下限保持 0，只改后三个
  const numbers = page.locator('.el-dialog .el-input-number input')
  await numbers.nth(1).fill('1000')
  await numbers.nth(2).fill('80')
  await numbers.nth(3).fill('300')
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('已保存')).toBeVisible()

  // 落库后表格里能看到该目的地
  await expect(page.getByRole('cell', { name: 'SG', exact: true })).toBeVisible()
})

test('税率规则页可以新增并显示计税口径', async ({ page }) => {
  await loginAdmin(page)
  await page.locator('.admin-menu').getByText('税率规则', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: '税率规则' })).toBeVisible()

  await page.getByRole('button', { name: '新增税率' }).click()
  await page.getByPlaceholder('国家码或市场辖区，如 SG / CN-VAT').fill('SG')
  await page.getByPlaceholder('如 增值税 13%').fill('GST 9%')
  await page.locator('.el-dialog .el-input-number input').fill('9')
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('已保存')).toBeVisible()

  await expect(page.getByRole('cell', { name: 'SG', exact: true })).toBeVisible()
  // 口径列必须显示"价内含税"，避免把净价口径误配
  await expect(page.getByText('价内含税').first()).toBeVisible()
})
