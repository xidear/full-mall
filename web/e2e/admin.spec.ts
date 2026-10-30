import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

/**
 * 守住的回归：后台每个菜单页仍能打开，且核心列表真的渲染出数据行（不是白屏/500）。
 * 依赖：41280 API + 41281 admin 已起、演示种子已灌；账号 admin / Admin@123456，
 * 验证码是后端出的数字加法题（无需 OCR）。失败先看 41281 能否打开、种子是否被重灌。
 */
test('admin can load dashboard, products, inventory and theme studio', async ({ page }) => {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  await expect(page.locator('.captcha-question')).toHaveText(/\d+ \+ \d+/)
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()

  await page.locator('.admin-menu').getByText('商品管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '商品管理' })).toBeVisible()
  // 不依赖具体演示数据标题（商品会随种子数据/排序变化）：只校验"列表真的渲染出商品行"
  await expect(page.locator('.el-table__body tr').first()).toBeVisible()

  await page.locator('.admin-menu').getByText('订单管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '订单管理' })).toBeVisible()

  await page.locator('.admin-menu').getByText('售后处理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '售后处理' })).toBeVisible()

  await page.locator('.admin-menu').getByText('客户管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '客户管理' })).toBeVisible()

  await page.locator('.admin-menu').getByText('门店收银', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '门店收银日结' })).toBeVisible()

  await page.locator('.admin-menu').getByText('仓储作业', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '仓储作业' })).toBeVisible()
  await expect(page.getByRole('tab', { name: '收货入库' })).toBeVisible()

  await page.locator('.admin-menu').getByText('供应商', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '供应商与开放接口凭证' })).toBeVisible()

  await page.locator('.admin-menu').getByText('会员与优惠券', { exact: true }).click()
  await expect(page.getByRole('heading', { name: '会员与优惠券' })).toBeVisible()
  await expect(page.getByText('会员管理')).toBeVisible()
  await page.getByRole('button', { name: '新建会员' }).click()
  const memberDialog = page.getByRole('dialog', { name: '新建会员' })
  await memberDialog.getByPlaceholder('请输入姓名').fill('E2E会员')
  // 手机号带时间戳后缀：会员手机号唯一，固定值第二次跑会撞唯一约束。
  await memberDialog.getByPlaceholder('请输入手机号').fill(`139${Date.now().toString().slice(-8)}`)
  await memberDialog.getByRole('button', { name: '创建' }).click()
  await expect(page.getByText('会员已创建')).toBeVisible()

  await page.locator('.admin-menu').getByText('促销管理', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '促销管理' })).toBeVisible()
  await expect(page.getByText('马克杯限时秒杀').first()).toBeVisible()
  await page.getByRole('button', { name: '新建促销' }).click()
  const promotionDialog = page.getByRole('dialog', { name: '新建促销' })
  const promoTitle = `E2E秒杀-${Date.now().toString().slice(-6)}`
  await promotionDialog.getByPlaceholder('例如：鲜奶买二赠一').fill(promoTitle)
  const numberInputs = promotionDialog.locator('.el-input-number input')
  await numberInputs.nth(1).fill('9.9')
  await promotionDialog.getByRole('button', { name: '创建并启用' }).click()
  await expect(page.getByText('促销已创建并生效')).toBeVisible()
  const promoRow = page.locator('.el-table__row', { hasText: promoTitle })
  // 用带时间戳的标题定位这一行，避免点到种子里的同名活动；用完立刻结束，别污染演示数据。
  await promoRow.getByRole('button', { name: '结束' }).click()
  await expect(page.getByText('活动已结束')).toBeVisible()

  await page.locator('.admin-menu').getByText('员工与权限', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '员工与数据权限' })).toBeVisible()
  await expect(page.getByText('上海静安店').first()).toBeVisible()

  await page.locator('.admin-menu').getByText('权限矩阵', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '角色权限矩阵' })).toBeVisible()
  await expect(page.getByText('商品管理', { exact: true }).first()).toBeVisible()

  await page.locator('.admin-menu').getByText('库存与批次', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '库存批次' })).toBeVisible()
  await expect(page.getByText('MILK-WH-20261015').first()).toBeVisible()

  await page.locator('.admin-menu').getByText('通知中心', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '通知中心' })).toBeVisible()

  await page.locator('.admin-menu').getByText('系统设置', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '系统设置' })).toBeVisible()

  await page.locator('.admin-menu').getByText('主题配置', { exact: true }).click()
  await expect(page.locator('h2', { hasText: '主题工作室' })).toBeVisible()
  await page.getByRole('button', { name: '机械模板' }).click()
  await expect(page.getByText('工业级选型 · 稳定交付')).toBeVisible()
})