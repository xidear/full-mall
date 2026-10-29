const symbols: Record<string, string> = {
  CNY: '¥',
  HKD: 'HK$',
  MOP: 'MOP$',
  TWD: 'NT$',
  USD: '$'
}

/**
 * 金额统一按“分”存储，展示时换算；货币符号按币种取，不写死人民币。
 * 小程序端不依赖 Intl（部分宿主不完整），用固定符号表更稳。
 */
export function formatMoney(amount: number | null | undefined, currency = 'CNY'): string {
  const value = Number(amount ?? 0) / 100
  const symbol = symbols[currency] ?? `${currency} `
  return `${symbol}${value.toFixed(2)}`
}
