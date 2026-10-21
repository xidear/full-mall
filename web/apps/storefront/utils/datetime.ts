/**
 * 顾客端时间格式：按**会员时区**显示（由 `/api/v1/member/auth/me` 提供）。
 *
 * <p>未登录或未取到时区时回退到浏览器本地时区——顾客看到自己设备的时间，比固定某个时区更合理。
 */
export function formatMemberDateTime(value: string | number | Date, timeZone?: string): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleString('zh-CN', timeZone ? { timeZone } : undefined)
}

export function formatMemberDate(value: string | number | Date, timeZone?: string): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleDateString('zh-CN', timeZone ? { timeZone } : undefined)
}
