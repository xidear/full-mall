/**
 * 语言切换：文案来自后端 /api/v1/i18n/{locale}，缺失键回退 zh-CN（后端负责）。
 * 语言写入 cookie，SSR 首屏与客户端一致，避免刷新后跳回中文。
 */
type Bundle = Record<string, string>

export const SUPPORTED_LOCALES = [
  { code: 'zh-CN', label: '简体中文' },
  { code: 'en', label: 'English' }
] as const

export type Locale = typeof SUPPORTED_LOCALES[number]['code']
const SUPPORTED = SUPPORTED_LOCALES.map(item => item.code) as readonly Locale[]
const COOKIE = 'fullmall-locale'

function normalize(value?: string | null): Locale {
  return SUPPORTED.includes(value as Locale) ? value as Locale : 'zh-CN'
}

export function useLocale() {
  const cookie = useCookie<string>(COOKIE, {
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 365
  })
  const locale = useState<Locale>('locale', () => normalize(cookie.value))
  const bundle = useState<Bundle>('locale-bundle', () => ({}))
  const error = useState<string>('locale-error', () => '')

  const apiBase = () => {
    const config = useRuntimeConfig()
    return import.meta.server ? config.apiInternalBase : config.public.apiBase
  }

  async function load(target?: Locale) {
    if (target && SUPPORTED.includes(target)) {
      locale.value = target
    }
    cookie.value = locale.value
    try {
      bundle.value = await $fetch<Bundle>(`${apiBase()}/api/v1/i18n/${locale.value}`)
      error.value = ''
    } catch (cause) {
      // 文案失败不能吞掉：记录错误，页面用 key 兜底，运维可在控制台看到
      error.value = cause instanceof Error ? cause.message : String(cause)
      bundle.value = {}
      console.error('[locale] i18n bundle 加载失败:', error.value)
    }
    if (import.meta.client) {
      localStorage.setItem('fullmall-locale', locale.value)
      document.documentElement.lang = locale.value
      document.documentElement.dir = locale.value === 'ar' ? 'rtl' : 'ltr'
    }
  }

  function t(key: string): string {
    return bundle.value[key] ?? key
  }

  async function init() {
    await load(normalize(cookie.value))
  }

  return {
    locale,
    bundle,
    t,
    load,
    init,
    error,
    supported: SUPPORTED,
    options: SUPPORTED_LOCALES
  }
}
