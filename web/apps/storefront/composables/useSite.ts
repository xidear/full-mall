import { computed } from 'vue'
import { VALIDATION } from '@fullmall/api-client'

/**
 * 站点级上下文：地区（market）、演示模式、品牌信息。
 *
 * <p>为什么集中在这里：页面里散落 `market: 'CN'`、`locale: 'zh-CN'` 会让
 * 多地区/多语言部署退化成"改代码才能出海"。地区策略只允许从这里读取。
 */
export type Market = 'CN' | 'HK' | 'MO' | 'TW'

const MARKETS: Market[] = ['CN', 'HK', 'MO', 'TW']

export function useSiteSettings() {
  const config = useRuntimeConfig()
  const base = import.meta.server ? config.apiInternalBase : config.public.apiBase
  return useAsyncData<Record<string, string>>('site-settings', () =>
    $fetch<Record<string, string>>(`${base}/api/v1/site/settings`)
  )
}

export function useSite() {
  const config = useRuntimeConfig()
  const settings = useState<Record<string, string>>('site-settings-state', () => ({}))
  const market = computed<Market>(() => {
    const value = settings.value.default_market
    return MARKETS.includes(value as Market) ? value as Market : 'CN'
  })
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
