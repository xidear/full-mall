const symbols: Record<string, string> = {
  CNY: '¥',
  HKD: 'HK$',
  MOP: 'MOP$',
  TWD: 'NT$',
  JPY: '¥',
  KRW: '₩',
  USD: '$',
  EUR: '€',
  GBP: '£'
}

/**
 * 币种最小单位（小数位）。小程序端不依赖 Intl（部分宿主不完整），用固定表；
 * 与服务端 `CurrencyPrecision` / 前端 `@fullmall/api-client/money` 保持同一组 ISO 例外。
 */
const minorUnits: Record<string, number> = {
  JPY: 0, KRW: 0, VND: 0, CLP: 0, ISK: 0,
  KWD: 3, BHD: 3, JOD: 3, OMR: 3, TND: 3, IQD: 3, LYD: 3
}

/**
 * 金额统一按最小单位整数存储，展示时按币种小数位换算；
 * 写死 /100 会把 JPY 放大 100 倍、KWD 少一位。
 */
export function formatMoney(amount: number | null | undefined, currency = 'CNY'): string {
  const code = currency.toUpperCase()
  const digits = minorUnits[code] ?? 2
  const value = Number(amount ?? 0) / 10 ** digits
  const symbol = symbols[code] ?? `${code} `
  return `${symbol}${value.toFixed(digits)}`
}