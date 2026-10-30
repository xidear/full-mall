import { expect, test } from '@playwright/test'
import { ADMIN_BASE, API_BASE } from './env'

/**
 * 市场三轴（ADR-0014）真实页面测试：
 * 1) 后台「市场配置」页能列出市场、能保存日本市场（币种 JPY / 语言 ja）
 * 2) 公开接口按市场返回语言候选，国别地址格式与区划字典可用（ADR-0013）
 *
 * 用 PUT 做 upsert，重复跑不会因为"已存在"失败。
 */
const API = API_BASE

async function loginAdmin(page: import('@playwright/test').Page) {
  await page.goto(ADMIN_BASE + '/login')
  await page.getByPlaceholder('请输入账号').fill('admin')
  await page.getByPlaceholder('请输入密码').fill('Admin@123456')
  const question = await page.locator('.captcha-question').innerText()
  const numbers = (question.match(/\d+/g) ?? []).map(Number)
  await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
  await page.getByRole('button', { name: '登录' }).click()
  await expect(page.getByRole('heading', { name: '经营总览' })).toBeVisible()
}

test('admin 市场配置页可编辑并保存市场（三轴：币种/语言/合规信息）', async ({ page }) => {
  await loginAdmin(page)

  await page.locator('.admin-menu').getByText('市场配置', { exact: true }).click()
  // 页头面包屑也叫「市场配置」，必须限定在 main 内，否则严格模式报歧义
  await expect(page.getByRole('main').getByRole('heading', { name: '市场配置' })).toBeVisible()

  // 种子里的四个市场必须都在（币种与市场是两回事：HK 用 HKD）
  await expect(page.getByRole('cell', { name: 'CN', exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'HK', exact: true })).toBeVisible()
  await expect(page.getByRole('cell', { name: 'HKD', exact: true })).toBeVisible()

  // 新建/编辑日本市场：币种 JPY、国家 JP、税辖区 JP、语言 ja + 主语言 ja
  await page.getByRole('button', { name: '新建市场' }).click()
  // 通道启用区必须在：这是"该市场用哪个服务商"的唯一入口（凭据在第三方服务页）
  await expect(page.getByText('通道启用')).toBeVisible()
  await page.getByPlaceholder('如 JP / HK / US').fill('JP')
  await page.getByPlaceholder('如 日本').fill('日本')
  await page.getByPlaceholder('ISO-4217，如 JPY').fill('JPY')
  await page.getByPlaceholder('ISO 3166-1 alpha-2，如 JP').fill('JP')
  await page.getByPlaceholder('如 JP / EU-OSS').fill('JP')
  await page.getByPlaceholder('IANA，如 Asia/Tokyo').fill('Asia/Tokyo')
  await page.getByPlaceholder('逗号分隔，如 ja,en').fill('ja,en')
  await page.getByPlaceholder('必须在语言候选内，如 ja').fill('ja')
  await page.getByRole('button', { name: '保存' }).click()

  await expect(page.getByText('已保存')).toBeVisible()
  await expect(page.getByRole('cell', { name: 'JP', exact: true }).first()).toBeVisible()
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
