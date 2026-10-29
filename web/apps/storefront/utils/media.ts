/**
 * 图片地址与失败兜底（storefront）。
 *
 * 约定与 admin 一致：http(s)/协议相对原样，站内路径按同源，其余补 /。
 * 失败时替换为中性占位图，避免裂图图标 + alt 文本撑破布局。
 */
export function resolveMediaUrl(raw?: string | null): string {
  if (!raw) return ''
  const value = raw.trim()
  if (!value) return ''
  if (value.startsWith('//')) return value
  if (/^https?:\/\//i.test(value)) {
    // 历史数据里的本机地址（localhost/127.0.0.1）对远程浏览器不可达，按页面同源重写
    if (typeof window !== 'undefined') {
      try {
        const url = new URL(value)
        if (['localhost', '127.0.0.1', '0.0.0.0'].includes(url.hostname)
            && url.hostname !== window.location.hostname) {
          return window.location.origin + url.pathname + url.search
        }
      } catch {
        return value
      }
    }
    return value
  }
  if (value.startsWith('/')) return value
  return '/' + value
}

const PLACEHOLDER = 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(
  '<svg xmlns="http://www.w3.org/2000/svg" width="320" height="240">' +
  '<rect width="320" height="240" fill="#f2f1ec"/>' +
  '<text x="160" y="126" font-family="sans-serif" font-size="14" fill="#8a8f8a" text-anchor="middle">无图</text>' +
  '</svg>')

export function onImageError(event: Event): void {
  const img = event.target as HTMLImageElement | null
  if (!img || img.dataset.fallbackApplied === 'true') return
  img.dataset.fallbackApplied = 'true'
  img.src = PLACEHOLDER
  img.alt = ''
}

