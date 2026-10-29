/**
 * 顾客端时间格式：按**会员时区**显示（由 `/api/v1/member/auth/me` 提供），
 * 并按当前界面语言本地化。
 *
 * <p>未登录或未取到时区时回退到浏览器本地时区——顾客看到自己设备的时间，比固定某个时区更合理。
 */
/**
 * 顾客端时间：时区由调用方显式传入，页面统一用 composables/useSite.ts 的 `useDisplayTimeZone()`
 * （会员时区 > 站点 `app.timezone` > Asia/Shanghai）。
 *
 * <p>不传就会落到服务器/浏览器本地时区：SSR 多在 UTC、客户端在顾客电脑上，
 * 同一订单会出现“首屏 08:00、水合后 16:00”的跳变，而且不报错。
 *
 * <p>入参是后端 ISO-8601（带偏移量）；解析失败返回空串而不是 `Invalid Date`——
 * 列表显示空白，不把浏览器英文错误文案甩给顾客，但数据缺陷要到接口返回里查。
 */
export function formatMemberDateTime(value: string | number | Date, timeZone?: string): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleString(currentLocale(), timeZone ? { timeZone } : undefined)
}

/** 只要日期：时区口径同上，避免“显示 10-28 还是 10-29”随机器变化。 */
export function formatMemberDate(value: string | number | Date, timeZone?: string): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleDateString(currentLocale(), timeZone ? { timeZone } : undefined)
}
