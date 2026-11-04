import { expect, test } from '@playwright/test'
import { ADMIN_BASE } from './env'

/**
 * 通道路由（ADR-0018）页面回归：
 * 运营配好供应商后，必须能在同一页按用途勾选「哪个动作用哪家」，并且能当场发一条测试消息。
 *
 * 依赖：41280 API + 41281 admin 已起，账号 admin / Admin@123456（验证码是后端出的加法题）。
 */
async function login(page: import('@playwright/test').Page) {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()
}

test('admin can bind a usage route and send a test message', async ({ page }) => {
  // 后台首屏要按需编译 + 需要登录/验证码：60s 不足以覆盖整条链路
  test.setTimeout(150_000)
  await login(page)
  await page.locator('.admin-menu').getByText('第三方服务', { exact: true }).click()
  await expect(page.getByRole('main').getByRole('heading', { name: '第三方服务' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '通道路由' })).toBeVisible()

  // 用途清单来自后端：至少要有登录验证码与告警这两个关键用途
  await expect(page.getByText('登录验证码').first()).toBeVisible()
  await expect(page.getByText('故障告警').first()).toBeVisible()

  // 邮件能力：先配一个 mock 供应商（演示占位），再绑用途，全程不联网
  const capabilitySelect = page.locator('.el-form-item', { hasText: '能力' }).first().locator('.el-select')
  await capabilitySelect.click()
  await page.locator('.el-select-dropdown__item', { hasText: '邮件' }).first().click()
  await page.locator('.el-form-item', { hasText: '服务商' }).first().locator('.el-select').click()
  await page.locator('.el-select-dropdown__item', { hasText: '演示占位' }).first().click()
  await page.locator('.el-form-item', { hasText: '启用' }).first().locator('.el-switch').click()
  await page.getByRole('button', { name: '保存' }).click()
  await expect(page.locator('.el-message').last()).toContainText('已保存')

  await page.reload()
  await expect(page.getByRole('heading', { name: '通道路由' })).toBeVisible()

  // 找到「登录验证码（邮件）」这一行：把供应商下拉切到 mock 并打开开关
  // 用途标签由后端下发（ChannelUsage）："登录验证码" + 描述"邮箱验证码登录" 定位到邮件那一行
  const row = page.locator('.el-table__row', { hasText: '邮箱验证码登录' }).first()
  await expect(row).toBeVisible()
  // 这个用例会被反复跑：上一轮可能已经绑好（开关是开的）。先把状态归一，
  // 否则「点开关」会变成解绑，断言「已绑定」就永远等不到。
  // Element Plus 的 role=switch 落在 0×0 的隐藏 input 上，要点可见的 .el-switch 容器。
  const boundSwitch = row.locator('.el-switch')
  if ((await row.locator('.el-switch.is-checked').count()) > 0) {
    await boundSwitch.click()
    await expect(page.locator('.el-message').last()).toContainText('已取消绑定')
  }

  await row.locator('.el-select').click()
  await page.locator('.el-select-dropdown__item', { hasText: '演示占位' }).first().click()
  // 未绑定的行：先选供应商，再打开「已绑定」开关才真正落库（避免误绑）
  await boundSwitch.click()
  await expect(page.locator('.el-message').last()).toContainText('已绑定')

  const boundRow = page.locator('.el-table__row', { hasText: '邮箱验证码登录' }).first()
  await boundRow.getByRole('button', { name: '测试发送' }).click()
  await expect(page.locator('.el-message').last()).toContainText('测试完成')
})

test('channel route table stays inside a phone viewport', async ({ page }) => {
  // 手机尺寸下侧边栏是收起状态，直接按路由进页面（运营在手机上就是点菜单，行为一致）
  await page.setViewportSize({ width: 390, height: 844 })
  await login(page)
  await page.goto(ADMIN_BASE + '/integrations')
  await expect(page.getByRole('main').getByRole('heading', { name: '第三方服务' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '通道路由' })).toBeVisible()

  // 手机尺寸下不允许整页横向溢出：宽表只能自己滚，不能把页面撑破
  const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth)
  expect(overflow).toBeLessThanOrEqual(1)

  // 标题必须横着排：只断言“没溢出”曾漏掉过标题被右侧 4 个控件挤成一列竖排的缺陷，
  // 所以这里连标题自身的盒子一起卡住（一行 24px 字高的标题宽高比不可能小于 2）。
  const titleBox = await page.locator('.admin-header h1').boundingBox()
  expect(titleBox).not.toBeNull()
  expect(titleBox!.width).toBeGreaterThanOrEqual(80)
  expect(titleBox!.height).toBeLessThanOrEqual(60)

  // 宽表自己可横向滚动（收件人/操作列在手机上靠滑动可达）
  const tableScrolls = await page.evaluate(() => {
    const wrap = document.querySelector('.el-scrollbar__wrap')
    return wrap === null ? -1 : wrap.scrollWidth - wrap.clientWidth
  })
  expect(tableScrolls).toBeGreaterThan(0)

  await page.screenshot({ path: 'test-results/channel-routes-mobile.png', fullPage: true })
})
