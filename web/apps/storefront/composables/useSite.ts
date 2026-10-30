import { computed } from 'vue'
import { VALIDATION } from '@fullmall/api-client'

/**
 * 站点级上下文：地区（market）、演示模式、品牌信息。
 *
 * <p>为什么集中在这里：页面里散落 `market: 'CN'`、`locale: 'zh-CN'` 会让
 * 多地区/多语言部署退化成"改代码才能出海"。地区策略只允许从这里读取。
 */
/** 市场码由后端数据决定（ADR-0014）：前端不再维护 CN/HK/MO/TW 白名单，否则加市场要改前端。 */
export type Market = string

/**
 * 当前访客所在市场的 cookie。
 *
 * <p>后端 /api/v1/site/settings 按 `X-Market` 决定"市场级覆盖全局"（如香港站不该显示大陆 ICP）。
 * 不带这个头就永远只拿到全局值——这正是"市场级备案不生效"的真正原因。
 */
export const MARKET_COOKIE = 'fullmall_market'

export function useSiteSettings() {
  const config = useRuntimeConfig()
  const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
  const market = useCookie<string | null>(MARKET_COOKIE)
  const headers = market.value ? { 'X-Market': market.value } : undefined
  return useAsyncData<Record<string, string>>('site-settings', () =>
    $fetch<Record<string, string>>(`${base}/api/v1/site/settings`, { headers })
  )
}

/**
 * 市场切换器：写 cookie 后整页重载，让 SSR 用新市场重新取设置（只改 cookie 不会触发重新取数）。
 * 候选来自 /api/v1/markets，前端不写死地区白名单。
 */
export function useMarketSwitch() {
  const config = useRuntimeConfig()
  const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
  const cookie = useCookie<string | null>(MARKET_COOKIE)
  const { data } = useAsyncData<Array<{ code: string; name: string }>>(
    'storefront-market-options',
    () => $fetch<Array<{ code: string; name: string }>>(`${base}/api/v1/markets`)
  )
  async function setMarket(code: string) {
    cookie.value = code
    await reloadNuxtApp({ persistState: false })
  }
  return {
    markets: computed(() => data.value ?? []),
    current: computed(() => cookie.value ?? ''),
    setMarket
  }
}

export function useSite() {
  const config = useRuntimeConfig()
  const settings = useState<Record<string, string>>('site-settings-state', () => ({}))
  // 不再做前端白名单校验、也不回退 'CN'：default_market 是否合法已由后端在保存时校验，
  // 这里再兜底一次只会掩盖"配置错了"这件事。
  const market = computed<Market>(() => settings.value.default_market?.trim() ?? '')
  const isMainland = computed(() => market.value === 'CN')
  const siteName = computed(() => settings.value.site_name?.trim() || 'Full Mall')
  const siteTagline = computed(() => settings.value.site_tagline?.trim() || '')
  const isDemo = computed(() => config.public.appMode === 'demo')
  const showAttribution = computed(() => (settings.value.branding_attribution || 'SHOW') !== 'HIDDEN')
  const showIcp = computed(() => isMainland.value && !!settings.value.icp_number)
  const showPublicSecurity = computed(() => isMainland.value && !!settings.value.public_security_record)
  const showBusinessLicense = computed(() => isMainland.value && !!settings.value.business_license_no)
  // 未登录时按站点默认时区显示；否则 SSR（服务器时区）与浏览器会显示不同时间。
  const timeZone = computed(() => settings.value['app.timezone']?.trim() || 'Asia/Shanghai')

  return {
    settings,
    market,
    isMainland,
    siteName,
    siteTagline,
    isDemo,
    showAttribution,
    showIcp,
    showPublicSecurity,
    showBusinessLicense,
    timeZone
  }
}

/** 会员时区优先；未登录或未设置时用站点默认时区，保证 SSR/客户端一致。 */
export function useDisplayTimeZone(): string {
  const memberTimezone = useCookie<string | null>('fullmall_member_timezone').value ?? ''
  const { timeZone } = useSite()
  return memberTimezone || timeZone.value
}

/**
 * 地区政策：同一套前台按市场切换合规文案与登录标识。
 * 当前后端会员登录仅支持手机号 OTP；邮箱/账号登录在地区能力成熟后接入，
 * 这里先把"由地区决定"的开关准备好，避免再次写死。
 */
/**
 * 当前市场的"国家"（Destination 默认值）。
 *
 * <p>市场 ≠ 国家（ADR-0014）：大陆与香港是不同市场，跨国市场还可能是 EU 这种多国组合，
 * 所以国家必须从 market 行上取，不能用市场码顶替。
 */
export function useMarketCountry() {
  const config = useRuntimeConfig()
  const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
  const { market } = useSite()
  const { data } = useAsyncData<Array<{ code: string; countryCode: string | null }>>(
    'storefront-markets',
    () => $fetch<Array<{ code: string; countryCode: string | null }>>(`${base}/api/v1/markets`)
  )
  return computed(() => {
    const current = (data.value ?? []).find(entry => entry.code === market.value)
    return current?.countryCode ?? ''
  })
}

export function useRegionPolicy() {
  const { market, isMainland } = useSite()
  const { t } = useLocale()
  const requiresAutoAccountNotice = computed(() => isMainland.value)
  const identifierHint = computed(() =>
    isMainland.value ? t('region.phone.cn') : t('region.phone.intl')
  )
  const phonePattern = new RegExp(VALIDATION.phonePattern)
  return { market, isMainland, requiresAutoAccountNotice, identifierHint, phonePattern }
}
