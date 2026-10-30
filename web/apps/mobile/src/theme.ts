import { computed, ref } from 'vue'
import {
  themeNames,
  themePresets,
  toCssVars,
  type ThemeConfig,
  type ThemePresetName
} from '@fullmall/theme'

/**
 * 移动端主题是跨页面共享的：每个页面都 `:style="themeVars"` 才真正生效。
 * 之前每个页面各自 `useMobileTheme()` 会创建独立状态，切了主题下一页又回到默认。
 */
const STORAGE_KEY = 'fullmall.mobile.theme'
const stored = (() => {
  try {
    const value = uni.getStorageSync(STORAGE_KEY) as ThemePresetName
    return themePresets[value] ? value : 'mall'
  } catch {
    return 'mall'
  }
})()

const theme = ref<ThemePresetName>(stored)
const market = ref('CN')
const remoteConfig = ref<ThemeConfig | null>(null)
// 远端配置优先于本地预设：运营在后台配了站点主题就以运营为准，
// 所以 setTheme 必须先清 remoteConfig 再改 theme，否则点了风格按钮看起来没反应。
/**
 * 后台主题工作室的草稿预览桥（H5 端）：iframe 父窗口用 postMessage 推 vars，
 * 直接覆盖页面内联变量——不落库、不写 storage，草稿不保存也不会污染线上配色。
 */
const previewVars = ref<Record<string, string> | null>(null)
if (typeof window !== 'undefined') {
  window.addEventListener('message', (event: MessageEvent) => {
    const data = event.data as { type?: string; vars?: Record<string, string> | null } | null
    if (!data || data.type !== 'fullmall:theme-preview') return
    previewVars.value = data.vars
  })
  if (window.parent !== window) {
    window.parent.postMessage({ type: 'fullmall:theme-preview-ready' }, '*')
  }
}

const vars = computed(() => previewVars.value
  ?? toCssVars(remoteConfig.value ?? themePresets[theme.value]))
const presets = themeNames().map(name => themePresets[name])
let remoteRequested = false

const apiBase = import.meta.env.VITE_API_BASE
  || `${location.protocol}//${location.hostname}:41280`

function setTheme(name: ThemePresetName) {
  if (!themePresets[name]) return
  remoteConfig.value = null
  theme.value = name
  uni.setStorageSync(STORAGE_KEY, name)
}

/**
 * 两步必须串行：主题接口按 market 取，market 需先由站点设置拿到，并行会用 CN 兜底问出错误配色。
 * 失败只降级到预置色板并写 console——这是「主题没生效」的唯一线索。
 */
function loadRemote() {
  if (remoteRequested) return
  remoteRequested = true
  // 主题按站点配置的市场取；拿不到时明确记录，再按 CN 默认。
  uni.request({
    url: `${apiBase}/api/v1/site/settings`,
    success: settingsResponse => {
      const settings = settingsResponse.data as { default_market?: string }
      market.value = settings.default_market || 'CN'
      uni.request({
        url: `${apiBase}/api/v1/theme`,
        data: { channel: 'MINI', market: market.value },
        success: response => {
          const payload = response.data as { config?: ThemeConfig | null }
          if (payload.config) {
            remoteConfig.value = payload.config
          }
        },
        fail: failure => console.error('[mobile] 主题加载失败:', failure.errMsg)
      })
    },
    fail: failure => console.error('[mobile] 站点设置加载失败:', failure.errMsg)
  })
}

export function useMobileTheme() {
  return { theme, market, vars, presets, setTheme, loadRemote }
}
