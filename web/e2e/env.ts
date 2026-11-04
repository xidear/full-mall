import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
/**
 * E2E 目标地址：默认值保持本地约定端口，可用环境变量覆盖。
 *
 * <p>为什么需要：spec 里曾把 41280/41281/41285 写死在 53 个文件里，
 * 于是"另一个会话占着端口"时整套 E2E 只能干等——现在可以指向任意一套环境：
 *
 * <pre>
 * E2E_API_BASE=http://127.0.0.1:41292 E2E_STOREFRONT_BASE=http://127.0.0.1:41294 \
 *   npx playwright test --project=chromium
 * </pre>
 */
function base(name: string, fallback: string): string {
  const value = process.env[name]
  return value && value.trim() ? value.trim().replace(/\/$/, '') : fallback
}

export const API_BASE = base('E2E_API_BASE', 'http://localhost:41280')
export const ADMIN_BASE = base('E2E_ADMIN_BASE', 'http://localhost:41281')
export const STORE_BASE = base('E2E_STORE_BASE', 'http://localhost:41282')
export const SUPPLIER_BASE = base('E2E_SUPPLIER_BASE', 'http://localhost:41283')
export const SUPPORT_BASE = base('E2E_SUPPORT_BASE', 'http://localhost:41284')
export const STOREFRONT_BASE = base('E2E_STOREFRONT_BASE', 'http://localhost:41285')
export const MOBILE_BASE = base('E2E_MOBILE_BASE', 'http://localhost:41286')

/**
 * 商城主机名（不含端口）：写 cookie 时必须与访问地址同域。
 *
 * <p>踩过的坑：spec 里写死 domain: 'localhost'，而 E2E 用 127.0.0.1 访问时浏览器不会带上该 cookie，
 * 页面表现就是"会员明明登录了却没有数据"。
 */
export const STOREFRONT_HOST = new URL(STOREFRONT_BASE).hostname

/**
 * 后台登录（账号固定 admin）：验证码是后端出的加法题，题目异步拉取且可能刷新。
 *
 * <p>踩过的坑：各 spec 手写「读题目 → 填答案 → 点登录」，题目刷新时会把旧答案提交给新题，
 * 页面停在登录页提示「请输入验证码」，用例就以「等不到经营总览」的形式失败（本轮 3 次被误判成产品缺陷）。
 * 这里改成填完再点、点完看结果，失败重试整轮，3 次都失败才抛错并给出排查方向。
 */
export async function login(page: Page): Promise<void> {
  const dashboard = page.getByRole('heading', { name: '经营总览' })
  const captchaError = page.locator('.el-alert', { hasText: '验证码' })
  for (let attempt = 1; attempt <= 3; attempt += 1) {
    await page.goto(ADMIN_BASE + '/login')
    await page.getByPlaceholder('请输入账号').fill('admin')
    await page.getByPlaceholder('请输入密码').fill('Admin@123456')
    await expect(page.locator('.captcha-question')).toHaveText(/\d+ \+ \d+/)
    const question = await page.locator('.captcha-question').innerText()
    const numbers = (question.match(/\d+/g) ?? []).map(Number)
    await page.getByPlaceholder('请输入验证码').fill(String(numbers[0] + numbers[1]))
    await page.getByRole('button', { name: '登录' }).click()
    await expect(dashboard.or(captchaError)).toBeVisible()
    if (await dashboard.isVisible()) {
      return
    }
  }
  throw new Error('后台登录失败：3 次尝试后仍停在登录页（先确认 41280 API 已起、演示种子已灌、账号 admin/Admin@123456）')
}
