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
// 语言写 cookie 而非只写 localStorage：SSR 首屏只能读 cookie，
// 否则刷新后服务端按 zh-CN 渲染、客户端再切英文，文案会闪一次。
const COOKIE = 'fullmall-locale'

// 新增文案在数据库迁移尚未执行时也要可读：关键键在前端保留兜底文案，
// 避免用户看到 footer.note.prod 这类内部 key。
const FALLBACK_COPY: Record<Locale, Record<string, string>> = {
  'zh-CN': {
    'footer.note.prod': '价格含税 · 门店自提 · 七天无理由',
    'checkout.login.title': '登录后继续结算',
    'checkout.login.desc': '登录或注册后将回到本页，购物车和填写内容都会保留。',
    'checkout.account.auto': '未注册的手机号验证后将自动创建账号，并同时同意《服务条款》与《隐私政策》。',
    'footer.attribution': 'Powered by Full Mall',
    'footer.region': '发货地区',
    'site.tagline': '全品类商城',
    'legal.current': '现行版本',
    'legal.upcoming': '即将生效',
    'legal.expired': '已失效',
    'legal.revision': '修订版本',
    'legal.effectiveFrom': '{date} 起生效',
    'legal.effectiveOn': '将于 {date} 生效',
    'legal.validUntil': '有效期至',
    'legal.fallback': '该语言暂无译本，当前展示简体中文版本。',
    'legal.footnote': '条款更新以标注的生效时间为准；新版本生效前，仍以现行版本为准。',
    'legal.type.TERMS': '服务条款',
    'legal.type.PRIVACY': '隐私政策',
    'legal.type.REFUND': '退款与售后政策',
    'legal.type.PICKUP': '门店自提规则',
    'product.tab.detail': '商品详情',
    'product.tab.specs': '规格参数',
    'product.tab.reviews': '用户评价',
    'product.tab.service': '售后保障',
    'product.quantity': '数量',
    'product.addToCart': '加入购物车',
    'product.buyNow': '立即购买',
    'product.favorite': '收藏',
    'product.favorited': '已收藏',
    'list.end.prefix': '已展示全部',
    'list.end.noun': '件商品',
    'list.end.reset': '清空筛选',
    'checkout.title': '结算',
    'checkout.items': '个商品',
    'checkout.fulfillment': '履约方式',
    'checkout.ship': '快递配送',
    'checkout.pickup': '门店自提',
    'checkout.store': '自提门店',
    'checkout.address': '收货地址',
    'checkout.addressPlaceholder': '请输入详细地址',
    'checkout.contact': '联系人',
    'checkout.contactPlaceholder': '请输入收货人姓名',
    'checkout.phone': '手机号',
    'checkout.summary': '订单摘要',
    'checkout.summaryHint': '秒杀、买赠与拼团价格由服务端结算时校验，最终应付以订单为准。',
    'checkout.total': '合计',
    'checkout.agree': '我已阅读并同意',
    'checkout.submit': '提交订单',
    'checkout.submitting': '提交中…',
    'checkout.loginToOrder': '登录后下单',
    'checkout.demoResult': '查看演示结果',
    'checkout.cartEmpty': '购物车为空',
    'checkout.legalFallback': '该语言暂无条款译本，当前以简体中文版本为准。',
    'checkout.termsLoadError': '服务条款加载失败，请刷新页面后重试。',
    'common.and': '和',
    'login.title': '手机号登录 / 注册',
    'login.phone': '手机号',
    'login.code': '短信验证码',
    'login.sendCode': '获取验证码',
    'login.sending': '发送中…',
    'login.submit': '登录 / 注册',
    'login.submitting': '登录中…',
    'nav.login': '登录 / 注册',
    'nav.member': '你好，会员',
    'checkout.phonePlaceholder': '请输入手机号',
    'login.autoAccount': '未注册手机号验证后自动创建会员账号。收藏、足迹、订单和消息都绑定该账号。',
    'login.agree': '登录即表示同意',
    'login.autoCreated': '；未注册手机号验证后将自动创建账号。',
    'region.phone.cn': '中国大陆手机号可直接填写 11 位；海外号码请带国家区号（如 +852）',
    'region.phone.intl': '请填写手机号（含国家区号，如 +852 / +853 / +886）'
  },
  en: {
    'footer.note.prod': 'Tax included · Store pickup · 7-day returns',
    'checkout.login.title': 'Sign in to continue',
    'checkout.login.desc': 'Sign in or register and you will return here. Your cart and entries are kept.',
    'checkout.account.auto': 'A new account is created automatically after phone verification, and you agree to the Terms and Privacy Policy.',
    'footer.attribution': 'Powered by Full Mall',
    'footer.region': 'Region',
    'site.tagline': 'All-category mall',
    'legal.current': 'Current version',
    'legal.upcoming': 'Effective soon',
    'legal.expired': 'Expired',
    'legal.revision': 'Revision',
    'legal.effectiveFrom': 'Effective from {date}',
    'legal.effectiveOn': 'Effective on {date}',
    'legal.validUntil': 'Valid until',
    'legal.fallback': 'No translation is available; showing the Simplified Chinese version.',
    'legal.footnote': 'Updates take effect on the stated date; until then the current version applies.',
    'legal.type.TERMS': 'Terms of service',
    'legal.type.PRIVACY': 'Privacy policy',
    'legal.type.REFUND': 'Refund & returns policy',
    'legal.type.PICKUP': 'Store pickup rules',
    'product.tab.detail': 'Details',
    'product.tab.specs': 'Specifications',
    'product.tab.reviews': 'Reviews',
    'product.tab.service': 'Service',
    'product.quantity': 'Qty',
    'product.addToCart': 'Add to cart',
    'product.buyNow': 'Buy now',
    'product.favorite': 'Save',
    'product.favorited': 'Saved',
    'list.end.prefix': 'All',
    'list.end.noun': 'items shown',
    'list.end.reset': 'Clear filters',
    'checkout.title': 'Checkout',
    'checkout.items': 'items',
    'checkout.fulfillment': 'Fulfillment',
    'checkout.ship': 'Shipping',
    'checkout.pickup': 'Store pickup',
    'checkout.store': 'Pickup store',
    'checkout.address': 'Address',
    'checkout.addressPlaceholder': 'Enter your address',
    'checkout.contact': 'Contact',
    'checkout.contactPlaceholder': 'Recipient name',
    'checkout.phone': 'Phone',
    'checkout.summary': 'Order summary',
    'checkout.summaryHint': 'Flash sale, gift and group-buy prices are verified at checkout; the order total applies.',
    'checkout.total': 'Total',
    'checkout.agree': 'I have read and agree to',
    'checkout.submit': 'Place order',
    'checkout.submitting': 'Submitting…',
    'checkout.loginToOrder': 'Sign in to order',
    'checkout.demoResult': 'View demo result',
    'checkout.cartEmpty': 'Your cart is empty',
    'checkout.legalFallback': 'No translated terms are available; the Simplified Chinese version applies.',
    'checkout.termsLoadError': 'Terms could not be loaded. Please refresh and try again.',
    'common.and': 'and',
    'login.title': 'Sign in / Register',
    'login.phone': 'Phone',
    'login.code': 'Verification code',
    'login.sendCode': 'Send code',
    'login.sending': 'Sending…',
    'login.submit': 'Sign in / Register',
    'login.submitting': 'Signing in…',
    'nav.login': 'Sign in / Register',
    'nav.member': 'Hello, member',
    'checkout.phonePlaceholder': 'Enter your phone number',
    'login.autoAccount': 'A new account is created after phone verification. Favorites, history, orders and messages are linked to it.',
    'login.agree': 'By signing in you agree to',
    'login.autoCreated': '; a new account is created after phone verification.',
    'region.phone.cn': 'Mainland China numbers can be entered as 11 digits; for other regions include the country code (e.g. +852).',
    'region.phone.intl': 'Enter your phone number with country code (e.g. +852 / +853 / +886).'
  }
}

// 只接受白名单语言：cookie 可被改成任意值，透传会去打 /api/v1/i18n/xx 换回 404，
// 让“语言包加载失败”的提示永久挂在页头。
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
    // 失败必须留痕：置 error 让 layout 提示顾客，console.error 保留原始错误给排障；
    // 只清空字典而不报错，顾客会看到 footer.note.prod 这类内部 key 却不知为何。
    try {
      bundle.value = await $fetch<Bundle>(`${apiBase()}/api/v1/i18n/${locale.value}`)
      error.value = ''
    } catch (cause) {
      // 文案失败不能吞掉：记录错误，页面用 key 兜底，运维可在控制台看到
      const detail = cause instanceof Error ? cause.message : String(cause)
      // 页面只显示人话；技术细节留在控制台，避免把请求路径/状态码抛给顾客。
      error.value = '语言包加载失败，已临时显示默认语言'
      bundle.value = {}
      console.error('[locale] i18n bundle 加载失败:', detail)
    }
    if (import.meta.client) {
      localStorage.setItem('fullmall-locale', locale.value)
      document.documentElement.lang = locale.value
      document.documentElement.dir = locale.value === 'ar' ? 'rtl' : 'ltr'
    }
  }

  // 三级回退：后端字典 > 前端兜底文案 > key 本身。退化成 key 是“可见的缺陷信号”，
  // 比空白更容易被发现，但上线前应补齐翻译。
  function t(key: string): string {
    return bundle.value[key] ?? FALLBACK_COPY[locale.value]?.[key] ?? key
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
