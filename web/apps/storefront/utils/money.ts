import { formatMoney as formatMoneyValue } from '@fullmall/api-client'
import type { Money } from '~/types'

/** 金额显示：最小单位整数 + 币种，小数位由 `@fullmall/api-client/money` 按币种决定。
 *  页面再写一次 `/100` 就是 100 倍误差（JPY 0 位、KWD 3 位）。
 *  空值返回 `-` 而非 0：0 是合法价格，显示 0 会让顾客以为免费。 */
export function formatMoney(money: Money | null | undefined): string {
  // 语言取 useState('locale') 而非 navigator.language：SSR 与客户端必须输出同一串，否则水合闪字。
  return formatMoneyValue(money, currentLocale())
}