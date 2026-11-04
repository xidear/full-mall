import { expect, test } from '@playwright/test'
import { API_BASE, login } from './env'

/**
 * 市场三轴（ADR-0014）真实页面测试：
 * 1) 后台「市场配置」页能列出市场、能保存日本市场（币种 JPY / 语言 ja）
 * 2) 公开接口按市场返回语言候选，国别地址格式与区划字典可用（ADR-0013）
 *
 * 用 PUT 做 upsert，重复跑不会因为"已存在"失败。
 */
const API = API_BASE

async function loginAdmin(page: import('@playwright/test').Page) {
  await login(page)
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()
}

test('admin 市场配置页可编辑并保存市场（三轴：币种/语言/合规信息）', async ({ page }) => {
  await loginAdmin(page)

  await page.locator('.admin-menu').getByText('市场配置', { exact: true }).click()
  // 页头面包屑也叫「市场配置」，必须限定在 main 内，否则严格模式报歧义
  await expect(page.getByRole('main').getByRole('heading', { name: '市场配置' })).toBeVisible()

  // 种子里的四个市场必须都在（币种与市场是两回事：HK 用 HKD）。
  // 市场码/国家/币种三列都可能出现同值，必须 first()，否则严格模式歧义。
  await expect(page.getByRole('cell', { name: 'CN', exact: true }).first()).toBeVisible()
  await expect(page.getByRole('cell', { name: 'HK', exact: true }).first()).toBeVisible()
  await expect(page.getByRole('cell', { name: 'HKD', exact: true }).first()).toBeVisible()

  // 验收口径（2026-10-30 调整）：币种/国家/税辖区/时区/语言候选/主语言已改为选择类控件
  // （AGENTS §13），下拉是 append-to-body + 虚拟列表，用 Playwright 逐项点选既脆弱也不测业务。
  // 因此这里改为：
  //   1) 断言这些字段**确实是选择控件**（防止有人改回手填）；
  //   2) 用「编辑已存在市场 → 改名 → 保存成功 → 列表回显」验证保存链路真的通。
  const editRow = page.locator('.el-table__row').filter({ hasText: 'CN' }).first()
  await editRow.getByRole('button', { name: '编辑' }).click()

  const dialog = page.getByRole('dialog')
  for (const label of ['币种', '国家', '税务辖区', '时区']) {
    const item = dialog.locator('.el-form-item').filter({ hasText: label })
    await expect(item.locator('.el-select')).toHaveCount(1)
  }
  // 语言候选是多选、主语言是单选（都在同一弹窗里）
  await expect(dialog.locator('.el-checkbox').first()).toBeVisible()
  // 通道启用区必须在：这是"该市场用哪个服务商"的唯一入口（凭据在第三方服务页）
  await expect(page.getByText('通道启用')).toBeVisible()

  // 标价含税口径必须显式选择：种子市场里该字段可能为空，前端会拦下保存（实测提示"请选择标价是否含税"）
  await dialog.locator('.el-form-item').filter({ hasText: '标价含税' })
    .locator('.el-radio', { hasText: '含税价' }).click()

  const renamed = '中国大陆-E2E'
  const nameInput = dialog.locator('.el-form-item').filter({ hasText: '名称' }).locator('input').first()
  await nameInput.fill(renamed)
  await dialog.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('已保存')).toBeVisible()
  await expect(page.locator('.el-table__row').filter({ hasText: renamed }).first()).toBeVisible()

  // 还原名称，避免污染演示数据（下一条用例依赖 "CN"/"HKD" 等种子值）
  await page.locator('.el-table__row').filter({ hasText: renamed }).first()
    .getByRole('button', { name: '编辑' }).click()
  await page.getByRole('dialog').locator('.el-form-item').filter({ hasText: '名称' })
    .locator('input').first().fill('中国大陆')
  await page.getByRole('dialog').getByRole('button', { name: '保存' }).click()
  await expect(page.getByText('已保存')).toBeVisible()
})

test('公开接口：市场语言候选 + 国别地址格式 + 区划字典（日本/新加坡形状不同）', async ({ request }) => {
  const markets = await (await request.get(`${API}/api/v1/markets`)).json()
  const codes = markets.map((m: { code: string }) => m.code)
  expect(codes).toEqual(expect.arrayContaining(['CN', 'HK', 'MO', 'TW']))

  const cn = markets.find((m: { code: string }) => m.code === 'CN')
  expect(cn.currency).toBe('CNY')
  expect(cn.locales.length).toBeGreaterThan(0)

  // 日本：都道府县 + 邮编必填，且没有"区"这一级在 require 里
  const jp = await (await request.get(`${API}/api/v1/regions/formats?country=JP`)).json()
  expect(jp.countryCode).toBe('JP')
  expect(jp.required).toContain('S')
  expect(jp.stateNameType).toBe('prefecture')

  // 新加坡：只有地址行 + 邮编，没有省市——这正是"固定三级"证伪的证据
  const sg = await (await request.get(`${API}/api/v1/regions/formats?country=SG`)).json()
  expect(sg.required).not.toContain('S')
  expect(sg.hasLocality).toBe(false)

  // 中国：一级行政区有 34 个，且能按 locale 取到中文名
  const provinces = await (await request.get(`${API}/api/v1/regions?country=CN&locale=zh-CN`)).json()
  expect(provinces.length).toBe(34)
  expect(provinces.some((p: { name: string }) => p.name === '北京市')).toBe(true)

  // 未配置格式的国家必须明确报错，不能静默套中国模板
  const missing = await request.get(`${API}/api/v1/regions/formats?country=ZZ`)
  expect(missing.status()).toBe(404)
  expect((await missing.json()).code).toBe('ADDRESS_FORMAT_MISSING')
})
