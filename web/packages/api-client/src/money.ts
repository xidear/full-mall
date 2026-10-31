/**
 * 金额显示/录入的唯一前端工具。
 *
 * <p>后端金额是最小单位整数（bigint），小数位由币种决定：JPY 0 位、多数 2 位、KWD 3 位。
 * 页面里再写一次 `/ 100` 就会把 JPY 放大 100 倍、KWD 少一位——这是资金事故。
 *
 * <p>小数位来自浏览器内置的 CLDR（`Intl.NumberFormat` 的 currency 选项），
 * 与服务端 `CurrencyPrecision`（ISO-4217）同源；SSR 与客户端结果一致，不需要额外请求。
 */
import type { Money } from './index'

export function currencyDigits(currency: string | null | undefined): number {
  const code = (currency ?? '').trim().toUpperCase()
  if (!code) {
    throw new Error('缺少币种，无法换算金额')
  }
  try {
    const options = new Intl.NumberFormat('en', { style: 'currency', currency: code })
      .resolvedOptions()
    return options.maximumFractionDigits ?? 2
  } catch {
    throw new Error('未知币种：' + code)
  }
}

/** 最小单位整数 → 主单位数值（用于输入框） */
export function toMajor(amountMinor: number, currency: string): number {
  return amountMinor / 10 ** currencyDigits(currency)
}

/** 主单位数值 → 最小单位整数（四舍五入到该币种精度） */
export function toMinor(amountMajor: number, currency: string): number {
  const digits = currencyDigits(currency)
  return Math.round(Number((amountMajor * 10 ** digits).toFixed(6)))
}

/**
 * 金额展示。缺失显示 `—`（0 是合法价格，不能显示成空/免费）。
 */
export function formatMoney(
  money: Money | null | undefined,
  locale = 'zh-CN'
): string {
  if (money == null || !Number.isFinite(money.amount)) {
    return '—'
  }
  const digits = currencyDigits(money.currency)
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: money.currency,
    minimumFractionDigits: digits,
    maximumFractionDigits: digits
  }).format(toMajor(money.amount, money.currency))
}

/** 只有金额+币种、没有 Money 对象时使用。 */
export function formatAmount(
  amountMinor: number | null | undefined,
  currency: string,
  locale = 'zh-CN'
): string {
  if (amountMinor == null || !Number.isFinite(amountMinor)) {
    return '—'
  }
  return formatMoney({ amount: amountMinor, currency }, locale)
}