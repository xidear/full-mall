/**
 * 语言切换（H9）：文案来自后端 `/api/v1/i18n/{locale}`，缺失键由后端回退 zh-CN。
 * 选择持久化到 localStorage；默认 zh-CN。
 */
type Bundle = Record<string, string>

const SUPPORTED = ['zh-CN', 'en'] as const
export type Locale = typeof SUPPORTED[number]

export function useLocale() {
  const locale = useState<Locale>('locale', () => 'zh-CN')
  const bundle = useState<Bundle>('locale-bundle', () => ({}))

  const apiBase = () => {
    const config = useRuntimeConfig()
    return import.meta.server ? config.apiInternalBase : config.public.apiBase
  }

  async function load(target?: Locale) {
    if (target && SUPPORTED.includes(target)) {
      locale.value = target
    }
    try {
      bundle.value = await $fetch<Bundle>(`${apiBase()}/api/v1/i18n/${locale.value}`)
      if (import.meta.client) {
        localStorage.setItem('fullmall-locale', locale.value)
      }
    } catch {
      // 文案拉取失败不阻断页面：t() 会退化为 key，至少不显示空白
      bundle.value = {}
    }
  }

  function t(key: string): string {
    return bundle.value[key] ?? key
  }

  async function init() {
    if (import.meta.client) {
      const saved = localStorage.getItem('fullmall-locale')
      if (saved && SUPPORTED.includes(saved as Locale)) {
        await load(saved as Locale)
        return
      }
    }
    await load()
  }

  return { locale, bundle, t, load, init, supported: SUPPORTED }
}
