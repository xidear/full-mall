import {
  currencyDigits,
  toMajor as sharedToMajor,
  toMinor as sharedToMinor
} from '@fullmall/api-client'

/**
 * 币种小数位（最小单位）——实现统一放在 `@fullmall/api-client/money`。
 *
 * <p>这里保留 composable 名称，避免页面大改；调用方仍拿 `minorUnits/toMajor/toMinor`。
 * 金额显示优先用 `formatMoney/formatAmount`，不要在页面里自己拼 `Intl` + `/100`。
 */
export function useCurrencyPrecision() {
  return {
    minorUnits: currencyDigits,
    toMajor: sharedToMajor,
    toMinor: sharedToMinor
  }
}