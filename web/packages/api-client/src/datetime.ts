/**
 * 后台/门店/客服/供应商端的统一时间格式。
 *
 * <p>为什么固定东八：管理端的一天必须是同一个"东八日"，否则运营在不同电脑上看到不同日期，
 * 对账（尤其是"今日订单/今日销售额"）会直接对不上。顾客端则按会员时区显示（另见 storefront）。
 */
export const ADMIN_TIME_ZONE = 'Asia/Shanghai'

export function formatDateTime(value: string | number | Date): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleString('zh-CN', { timeZone: ADMIN_TIME_ZONE })
}

export function formatDate(value: string | number | Date): string {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  return date.toLocaleDateString('zh-CN', { timeZone: ADMIN_TIME_ZONE })
}
