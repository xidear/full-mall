import { execFileSync } from 'node:child_process'
import { expect, test } from '@playwright/test'
import { ADMIN_BASE, login } from './env'

/**
 * 风险告警页（/alerts）：站内信默认通道可见、确认处理能停掉升级、恢复能收掉告警。
 *
 * <p>为什么用 psql 造数据：演示模式不会自然产生 FATAL 告警（支付告警只在生产触发、
 * 物流告警是 MEDIUM），而这条链路必须被验证。容器名可用 E2E_DB_CONTAINER 覆盖
 * （与 e2e 收尾脚本同一个变量），容器不存在时**直接失败**，不做跳过。
 */
const DB_CONTAINER = process.env.E2E_DB_CONTAINER ?? 'fullmall-pg'

function sql(statement: string): void {
  execFileSync('docker', ['exec', DB_CONTAINER, 'psql', '-U', 'fullmall', '-d', 'fullmall',
    '-v', 'ON_ERROR_STOP=1', '-q', '-c', statement], { stdio: 'pipe' })
}

const ALERT_ID = '22222222-2222-2222-2222-222222222222'

function seedPendingAlert(): void {
  sql(`DELETE FROM risk_alert WHERE id = '${ALERT_ID}'`)
  // alerted/alert_channel 模拟派发后的样子：站内信成功、外部通道未配置
  sql(`INSERT INTO risk_alert (id, rule_code, severity, subject, detail, alerted, alerted_at, alert_channel, created_at)
       VALUES ('${ALERT_ID}', 'E2E_UI_ALERT', 'FATAL', 'e2e-alerts-subject', '页面用例构造的告警',
               true, now(), 'IN_APP', now())`)
}

test('告警页展示站内信默认通道，并能确认处理与恢复', async ({ page }) => {
  seedPendingAlert()
  await login(page)

  await page.locator('.admin-menu').getByText('风险告警', { exact: true }).click()
  // 页头与页面各有一个 h1（页头显示路由标题），断言必须限定在 main 里
  await expect(page.getByRole('main').getByRole('heading', { name: '风险告警' })).toBeVisible()
  await expect(page.getByText(/站内信是默认通道/)).toBeVisible()

  const row = page.locator('.el-table__row', { hasText: 'E2E_UI_ALERT' }).first()
  await expect(row).toBeVisible()
  await expect(row).toContainText('FATAL')
  await expect(row).toContainText('待处理')
  // 站内信是默认通道：已推送通道里必须有 IN_APP
  await expect(row).toContainText('IN_APP')

  await row.getByRole('button', { name: '确认处理' }).click()
  // 登录成功的 toast 可能还在屏上：按文案定位，避免 strict mode 命中两个 .el-message
  await expect(page.locator('.el-message', { hasText: '已确认' })).toBeVisible()
  const acked = page.locator('.el-table__row', { hasText: 'E2E_UI_ALERT' }).first()
  await expect(acked).toContainText('处理中')
  await expect(acked).toContainText('admin')

  await acked.getByRole('button', { name: '标记恢复' }).click()
  await page.locator('.el-message-box').getByRole('button', { name: '标记恢复' }).click()
  await expect(page.locator('.el-message', { hasText: '已标记恢复' })).toBeVisible()
  await expect(page.locator('.el-table__row', { hasText: 'E2E_UI_ALERT' }).first())
    .toContainText('已恢复')
})

test('告警派发后管理端能看到站内信（默认通道）', async ({ page }) => {
  seedPendingAlert()
  await login(page)
  // 直接查接口而不是渲染铃铛：铃铛有分页与折叠，断言会落在 UI 细节上而不是「站内信有没有送到」
  const token = await page.evaluate(() => localStorage.getItem('fullmall.admin.token'))
  expect(token).toBeTruthy()
  const response = await page.request.get(`${ADMIN_BASE}/admin/v1/notifications?limit=20&channel=IN_APP`,
    { headers: { Authorization: `Bearer ${token}` } })
  expect(response.ok()).toBeTruthy()
  const rows = await response.json() as Array<{ eventType: string; channel: string; link?: string }>
  expect(rows.every(item => item.channel === 'IN_APP')).toBeTruthy()
  const alertRows = rows.filter(item => item.eventType.startsWith('ALERT_'))
  for (const item of alertRows) {
    // 告警类站内信必须能点回告警页，否则「看到了但找不到去哪处理」
    expect(item.link).toBe('/alerts')
  }
})

test.describe('手机尺寸', () => {
  test.use({ viewport: { width: 390, height: 844 }, isMobile: true, hasTouch: true })

  test('手机尺寸下改为卡片列表：关键字段不用横向滚动就能看全，确认处理可用', async ({ page }) => {
    seedPendingAlert()
    await login(page)
    // 手机尺寸下侧边栏是收起状态，菜单文字不可见：直接进页面（这条用例验的是页面本身）
    await page.goto(ADMIN_BASE + '/alerts')
    await expect(page.getByRole('main').getByRole('heading', { name: '风险告警' })).toBeVisible()

    // 宽表格在 390px 下会被右侧固定列吃掉半屏，所以窄屏走卡片；这里断言卡片而不是表格行
    await expect(page.locator('.alert-table')).toHaveCount(0)
    const card = page.locator('.alert-card', { hasText: 'E2E_UI_ALERT' }).first()
    await card.scrollIntoViewIfNeeded()
    await expect(card).toContainText('FATAL')
    await expect(card).toContainText('待处理')
    await expect(card).toContainText('IN_APP')

    await card.getByRole('button', { name: '确认处理' }).click()
    await expect(page.locator('.el-message', { hasText: '已确认' })).toBeVisible()
    await expect(page.locator('.alert-card', { hasText: 'E2E_UI_ALERT' }).first())
      .toContainText('处理中')
  })
})
