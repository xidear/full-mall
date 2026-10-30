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
