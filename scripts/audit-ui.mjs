import playwright from '../web/node_modules/playwright-core/index.js'
import { readFileSync } from 'node:fs'

const { chromium } = playwright

/**
 * 后台页面"设计规范"审计（配色对比度 / 裂图 / 控制台报错 / 横向溢出）。
 *
 * 为什么需要：接口 200 不代表页面可读。2026-10-28 出现过"深色侧栏 + 深色文字"
 * （Element Plus 的 --el-menu-text-color 没被覆盖），接口全绿但菜单几乎看不见。
 *
 * 用法：
 *   node scripts/audit-ui.mjs                 # 全部路由
 *   node scripts/audit-ui.mjs --min 2.5       # 对比度阈值（默认 3，WCAG AA 大字号）
 *   node scripts/audit-ui.mjs --shots docs/review-2026-10-28   # 顺带截图
 *
 * 依赖：web/node_modules 里的 playwright-core + 本机 Playwright chromium
 *（CI 里需先 npx playwright install chromium）。
 */

const API = process.env.API_BASE || 'http://127.0.0.1:41280'
const ADMIN = process.env.ADMIN_BASE || 'http://127.0.0.1:41281'
const args = process.argv.slice(2)
const minContrast = Number((args.find(a => a.startsWith('--min=')) || '--min=3').split('=')[1])
const shotsArg = args.find(a => a.startsWith('--shots='))
const shotsDir = shotsArg ? shotsArg.split('=')[1] : ''

async function login() {
  const captcha = await (await fetch(API + '/admin/v1/auth/captcha')).json()
  const numbers = (captcha.question.match(/\d+/g) || []).map(Number)
  const response = await fetch(API + '/admin/v1/auth/login', {
    method: 'POST',
    headers: { 'content-type': 'application/json' },
    body: JSON.stringify({
      username: process.env.ADMIN_USER || 'admin',
      password: process.env.ADMIN_PASSWORD || 'Admin@123456',
      captchaId: captcha.captchaId,
      captchaAnswer: String(numbers[0] + numbers[1])
    })
  })
  const body = await response.json()
  if (!body.token) throw new Error('登录失败：' + JSON.stringify(body))
  return body.token
}

function routes() {
  const source = readFileSync(new URL('../web/apps/admin/src/router.ts', import.meta.url), 'utf8')
  const paths = Array.from(source.matchAll(/path:\s*'([^']+)'/g)).map(m => m[1])
  return paths
    .filter(p => p !== '/login' && p !== '*')
    .map(p => p.replace(/:id/g, 'aaaa0000-0000-0000-0000-000000000004'))
}

const CONTRAST_SCRIPT = (minRatio) => {
  const parse = (value) => {
    const nums = (value.match(/[\d.]+/g) || []).map(Number)
    if (nums.length < 3) return null
    return { r: nums[0], g: nums[1], b: nums[2], a: nums.length > 3 ? nums[3] : 1 }
  }
  const over = (fg, bg) => ({
    r: fg.r * fg.a + bg.r * (1 - fg.a),
    g: fg.g * fg.a + bg.g * (1 - fg.a),
    b: fg.b * fg.a + bg.b * (1 - fg.a)
  })
  const luminance = (c) => {
    const channel = (v) => {
      const x = v / 255
      return x <= 0.03928 ? x / 12.92 : Math.pow((x + 0.055) / 1.055, 2.4)
    }
    return 0.2126 * channel(c.r) + 0.7152 * channel(c.g) + 0.0722 * channel(c.b)
  }
  const ratio = (a, b) => {
    const l1 = luminance(a)
    const l2 = luminance(b)
    const hi = Math.max(l1, l2)
    const lo = Math.min(l1, l2)
    return (hi + 0.05) / (lo + 0.05)
  }
  const backgroundOf = (element) => {
    let node = element
    let stack = []
    while (node && node !== document.documentElement) {
      const bg = parse(getComputedStyle(node).backgroundColor)
      if (bg && bg.a > 0) {
        stack.push(bg)
        if (bg.a >= 1) break
      }
      node = node.parentElement
    }
    let result = { r: 255, g: 255, b: 255, a: 1 }
    for (let i = stack.length - 1; i >= 0; i -= 1) result = over(stack[i], result)
    return result
  }
  const selector = 'h1,h2,h3,h4,p,span,td,th,label,a,button,li,.el-menu-item,.el-sub-menu__title'
  const results = []
  for (const element of Array.from(document.querySelectorAll(selector))) {
    const text = (element.textContent || '').trim()
    if (!text) continue
    if (element.children.length > 0 && element.childNodes.length > 1) {
      // 只检查"自身直接包含文本"的元素，避免父容器重复计算
      const own = Array.from(element.childNodes).filter(n => n.nodeType === 3).map(n => n.textContent.trim()).join('')
      if (!own) continue
    }
    const rect = element.getBoundingClientRect()
    if (rect.width < 2 || rect.height < 2) continue
    const style = getComputedStyle(element)
    if (style.visibility === 'hidden' || style.display === 'none' || Number(style.opacity) < 0.4) continue
    const fg = parse(style.color)
    if (!fg) continue
    if (fg.a === 0) continue
    const bg = backgroundOf(element)
    const effective = over(fg, bg)
    const value = ratio(effective, bg)
    if (value < minRatio) {
      results.push({
        text: text.slice(0, 24),
        ratio: Math.round(value * 100) / 100,
        color: style.color,
        background: 'rgb(' + Math.round(bg.r) + ',' + Math.round(bg.g) + ',' + Math.round(bg.b) + ')',
        className: String(element.className).slice(0, 60)
      })
    }
  }
  return results.slice(0, 8)
}

const LAYOUT_SCRIPT = () => {
  const CONTROL = '.el-input, .el-select, .el-input-number, .el-button, .el-checkbox, .el-radio-group, .el-switch, .el-tag, .el-textarea, .admin-thumb, .asset-image-fallback, img'
  const containers = new Set()
  document.querySelectorAll(CONTROL).forEach((el) => { if (el.parentElement) containers.add(el.parentElement) })
  const issues = []
  for (const container of containers) {
    const style = getComputedStyle(container)
    if (!/flex|grid/.test(style.display)) continue
    const kids = Array.from(container.children).filter((el) => el.matches(CONTROL))
    if (kids.length < 2) continue
    const measured = kids
      .map((el) => ({ className: String(el.className || el.tagName).slice(0, 36), rect: el.getBoundingClientRect() }))
      .filter((item) => item.rect.width > 0 && item.rect.height > 0)
    // 按"垂直中心"聚成行：中心相差 8px 内视为同一行（排除换行导致的上下两行）
    const rows = []
    for (const item of measured) {
      const center = item.rect.top + item.rect.height / 2
      const row = rows.find((candidate) => Math.abs(candidate.center - center) <= 8)
      if (row) {
        row.items.push(item)
        row.center = row.items.reduce((sum, entry) => sum + entry.rect.top + entry.rect.height / 2, 0) / row.items.length
      } else {
        rows.push({ center, items: [item] })
      }
    }
    for (const row of rows) {
      if (row.items.length < 2) continue
      const heights = row.items.map((item) => Math.round(item.rect.height))
      const tops = row.items.map((item) => Math.round(item.rect.top))
      const topSpread = Math.max(...tops) - Math.min(...tops)
      // el-tag 是徽标/标签块，按设计就比输入框矮（24 vs 32）：不参与"高度一致"比较，只校验是否同一行
      const heightCandidates = row.items
        .filter((item) => !item.className.includes('el-tag'))
        .map((item) => Math.round(item.rect.height))
      const heightSpread = heightCandidates.length >= 2
        ? Math.max(...heightCandidates) - Math.min(...heightCandidates)
        : 0
      if (heightSpread > 4) {
        issues.push({
          reason: '同一行控件高度不一致',
          heights,
          tops,
          sample: row.items.map((item) => item.className),
          container: String(container.className || container.tagName).slice(0, 40)
        })
      } else if (topSpread > 4) {
        issues.push({
          reason: '同一行控件未对齐（top 差）',
          heights,
          tops,
          sample: row.items.map((item) => item.className),
          container: String(container.className || container.tagName).slice(0, 40)
        })
      }
    }
  }
  return issues.slice(0, 8)
}

const token = await login()
const browser = await chromium.launch()
const context = await browser.newContext({ viewport: { width: 1440, height: 900 } })
await context.addInitScript((value) => localStorage.setItem('fullmall.admin.token', value), token)
const page = await context.newPage()

async function collect() {
  return page.evaluate(
    ({ script, min, layoutScript }) => {
      const runContrast = new Function('min', 'return (' + script + ')(min)')
      const runLayout = new Function('return (' + layoutScript + ')()')
      const images = Array.from(document.images)
        .filter(img => img.getAttribute('src') && img.complete && img.naturalWidth === 0)
        .map(img => img.getAttribute('src'))
      const overflow = document.scrollingElement.scrollWidth - document.scrollingElement.clientWidth
      return { brokenImages: images, contrast: runContrast(min), overflow, layout: runLayout() }
    },
    { script: CONTRAST_SCRIPT.toString(), min: minContrast, layoutScript: LAYOUT_SCRIPT.toString() })
}

function summarize(report, consoleErrors, httpErrors) {
  const problems = []
  if (report.brokenImages.length) problems.push('裂图 ' + report.brokenImages.length + ' 张：' + report.brokenImages.slice(0, 3).join(', '))
  if (report.contrast.length) problems.push('低对比度 ' + report.contrast.length + ' 处：' + JSON.stringify(report.contrast.slice(0, 2)))
  if (report.overflow > 2) problems.push('横向溢出 ' + report.overflow + 'px')
  if (report.layout.length) problems.push('对齐问题 ' + report.layout.length + ' 处：' + JSON.stringify(report.layout.slice(0, 2)))
  if (consoleErrors.length) problems.push('控制台报错 ' + consoleErrors.length + ' 条：' + consoleErrors[0].slice(0, 80))
  if (httpErrors.length) problems.push('HTTP 错误：' + httpErrors.slice(0, 2).join(' | '))
  return problems
}

/**
 * 需要"点开才能看到"的界面：弹窗与标签页。
 * 只扫路由本身会漏掉所有 dialog（"选素材没对齐""维度行没对齐"两次都发生在弹窗里）。
 */
const INTERACTIONS = [
  {
    route: '/products',
    label: '规格改价弹窗',
    open: async () => {
      await page.locator('.el-table__body tr').first().getByRole('button', { name: '规格改价' }).click()
    }
  },
  {
    route: '/sku-matrix',
    label: '新建模板弹窗',
    open: async () => {
      await page.getByRole('button', { name: '新建模板' }).click()
      await page.waitForTimeout(400)
      await page.locator('input[placeholder="维度名，例如 颜色"]').first().fill('颜色')
      const valueInput = page.locator('input[placeholder="输入取值后回车"]').first()
      for (const value of ['黑', '白', '金']) {
        await valueInput.fill(value)
        await valueInput.press('Enter')
      }
    }
  },
  {
    route: '/products/aaaa0000-0000-0000-0000-000000000004/edit',
    label: '译文标签页',
    open: async () => {
      await page.getByRole('tab', { name: 'English' }).click()
    }
  },
  {
    route: '/products/aaaa0000-0000-0000-0000-000000000004/edit',
    label: '详情标签页（富文本）',
    open: async () => {
      await page.getByRole('tab', { name: '详情' }).click()
    }
  },
  {
    route: '/products/aaaa0000-0000-0000-0000-000000000004/edit',
    label: '规格标签页',
    open: async () => {
      await page.getByRole('tab', { name: '规格' }).click()
    }
  }
]

let failures = 0
for (const route of routes()) {
  const consoleErrors = []
  const httpErrors = []
  const onConsole = (message) => {
    if (message.type() !== 'error') return
    // 浏览器对 404 会额外打一条 "Failed to load resource"，与 httpErrors 重复计数
    if (/Failed to load resource/.test(message.text())) return
    consoleErrors.push(message.text())
  }
  const expected404 = ['/admin/v1/ai/status', '/admin/v1/ai/ask']
  const onResponse = (response) => {
    if (response.status() < 400) return
    if (response.status() === 404 && expected404.some(path => response.url().includes(path))) return
    httpErrors.push(response.status() + ' ' + response.url())
  }
  const onFailed = (request) => { httpErrors.push('FAILED ' + request.url() + ' (' + (request.failure() || {}).errorText + ')') }
  page.on('console', onConsole)
  page.on('response', onResponse)
  page.on('requestfailed', onFailed)
  await page.goto(ADMIN + route, { waitUntil: 'networkidle' })
  await page.waitForTimeout(600)
  const report = await collect()
  if (shotsDir) await page.screenshot({ path: shotsDir + '/audit-' + route.replace(/[/]/g, '_') + '.png' })

  const problems = summarize(report, consoleErrors, httpErrors)
  for (const interaction of INTERACTIONS.filter(item => item.route === route)) {
    try {
      await interaction.open()
      await page.waitForTimeout(900)
      const inner = await collect()
      const innerProblems = summarize(inner, [], [])
      if (shotsDir) {
        await page.screenshot({ path: shotsDir + '/audit-' + route.replace(/[/]/g, '_') + '-' + interaction.label + '.png' })
      }
      for (const problem of innerProblems) problems.push(interaction.label + '：' + problem)
      if (interaction.label.includes('弹窗')) {
        await page.keyboard.press('Escape')
        await page.waitForTimeout(300)
      }
    } catch (cause) {
      problems.push(interaction.label + '：打不开（' + String(cause.message).slice(0, 80) + '）')
    }
  }

  page.off('console', onConsole)
  page.off('response', onResponse)
  page.off('requestfailed', onFailed)

  if (problems.length) failures += 1
  console.log((problems.length ? 'FAIL ' : 'ok   ') + route + (problems.length ? '  -> ' + problems.join(' ; ') : ''))
}
await browser.close()
console.log('\n共 ' + routes().length + ' 个路由，' + failures + ' 个有问题')
process.exit(failures ? 1 : 0)

