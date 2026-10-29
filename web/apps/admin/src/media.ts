/**
 * 图片地址解析（唯一入口）。
 *
 * 约定（设计审查 2026-10-28）：
 * 1. 后端返回的图片地址可能是 http(s) 绝对地址、协议相对地址，或以 / 开头的站内路径；
 * 2. 站内路径必须以 **API 同源** 为准，不能以"某个前端的 public 目录"为准——
 *    演示商品的 SVG 只在 storefront 的 public 下，admin 直接用它必然 404（裂图根因）；
 *    现在由 API 提供 /images/**，admin / storefront 的 dev server 与 nginx 都转发到 API；
 * 3. 地址无法识别时返回空串，由组件显示占位图，而不是把坏地址塞给 <img>。
 */
export function resolveMediaUrl(raw?: string | null): string {
  if (!raw) return ''
  const value = raw.trim()
  if (!value) return ''
  if (value.startsWith('//')) return value
  if (/^https?:\/\//i.test(value)) {
    // 历史数据把本机地址写进了图片 URL（http://localhost:8080/media/...）。
    // 对远程浏览器 localhost 指向它自己的电脑 → 连接被拒。这里按"页面同源"重写主机部分。
    return rewriteLocalHost(value)
  }
  if (value.startsWith('/')) return value
  // 后端历史上存过不带前导斜杠的相对路径（如 images/x.svg）
  return '/' + value
}

function rewriteLocalHost(value: string): string {
  try {
    const url = new URL(value, window.location.origin)
    const localHosts = ['localhost', '127.0.0.1', '0.0.0.0']
    if (localHosts.includes(url.hostname) && url.hostname !== window.location.hostname) {
      return window.location.origin + url.pathname + url.search
    }
    return value
  } catch {
    return value
  }
}
