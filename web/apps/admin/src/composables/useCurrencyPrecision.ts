import { ref } from 'vue'
import { api } from '../api'

/**
 * 币种小数位（最小单位）——来自后端 /api/v1/currencies/precision。
 *
 * <p>金额在库里是最小单位整数，页面换算必须按币种来：日元 0 位、多数币种 2 位、
 * 第纳尔 3 位。写死 ×100 会让 JPY 市场金额放大 100 倍——运费/结算上是资金事故。
 */
interface PrecisionPayload { defaultMinorUnits: number; overrides: Record<string, number> }

const payload = ref<PrecisionPayload | null>(null)
let requested = false

async function ensureLoaded() {
  if (requested) return
  requested = true
  try {
    payload.value = await api.get<PrecisionPayload>('/api/v1/currencies/precision')
  } catch {
    // 取不到就明确暴露：调用方拿到 null 时用 minorUnits() 会抛错，而不是悄悄按 2 位算
    payload.value = null
  }
}

export function useCurrencyPrecision() {
  void ensureLoaded()

  function minorUnits(currency?: string | null): number {
    if (!payload.value) {
      throw new Error('币种小数位未加载：请刷新页面重试')
    }
    const code = (currency ?? '').trim().toUpperCase()
    if (!code) {
      throw new Error('缺少币种，无法换算金额')
    }
    return payload.value.overrides[code] ?? payload.value.defaultMinorUnits
  }

  /** 最小单位整数 → 主单位数值（用于输入框） */
  function toMajor(amountMinor: number, currency?: string | null): number {
    return amountMinor / 10 ** minorUnits(currency)
  }

  /** 主单位数值 → 最小单位整数（四舍五入到该币种精度） */
  function toMinor(amountMajor: number, currency?: string | null): number {
    const units = minorUnits(currency)
    return Math.round(Number((amountMajor * 10 ** units).toFixed(6)))
  }

  return { minorUnits, toMajor, toMinor }
}
