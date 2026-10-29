import type { Money } from '~/types'

/** 金额显示：`amount` 单位是**分**，换算只允许在这里做——页面再写一次 `/ 100` 就是 100 倍误差。
 *  空值返回 `-` 而非 0：0 是合法价格，显示 0 会让顾客以为免费。 */
export function formatMoney(money: Money | null | undefined): string {
  if (!money) {
    return '-'
  }
  // 语言取 useState('locale') 而非 navigator.language：SSR 与客户端必须输出同一串，否则水合闪字。
  return new Intl.NumberFormat(currentLocale(), {
    style: 'currency',
    currency: money.currency
  }).format(money.amount / 100)
}
