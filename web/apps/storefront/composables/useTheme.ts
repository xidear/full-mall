import {
  themePresets,
  toCssVars,
  type ThemePresetName
} from '@fullmall/theme'

/**
 * 主题必须让 SSR 与客户端看到同一个值：否则首屏用模板 A 的内联变量、客户端用模板 B 的
 * data-theme，会出现“背景变了但文字还是旧颜色”的半套主题（Vue 水合不补 style）。
 *
 * 优先级：URL `?theme=`（预览/验收）> cookie（用户选择，SSR 可读）> 构建默认值。
 *
 * <p>为什么不只放 localStorage：服务端读不到，首屏会先用默认模板渲染、再被客户端替换一次。
 */
export function useTheme() {
  const config = useRuntimeConfig()
  const route = useRoute()
  const cookie = useCookie<ThemePresetName | null>('fullmall_theme', {
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 365
  })

  // URL ?theme= 只用于预览/验收（不落 cookie），优先级最高且绕过服务端保存的主题配置。
  const queryTheme = typeof route.query.theme === 'string' ? route.query.theme : ''
  const fromQuery = themePresets[queryTheme as ThemePresetName] ? queryTheme as ThemePresetName : null
  const fromCookie = cookie.value && themePresets[cookie.value] ? cookie.value : null
  const fallback = (config.public.theme as ThemePresetName) || 'mall'
  const initial = fromQuery ?? fromCookie ?? (themePresets[fallback] ? fallback : 'mall')

  const theme = useState<ThemePresetName>('theme', () => initial)
  if (fromQuery) {
    theme.value = fromQuery
  }

  // 同时落 cookie 与 localStorage：cookie 供下一次 SSR 首屏使用，localStorage 便于排查
  // “为什么改了没生效”。写入是覆盖式，不需要失效策略。
  watch(theme, value => {
    cookie.value = value
    if (import.meta.client) {
      localStorage.setItem('fullmall.theme', value)
    }
  })

  // 主题以 CSS 变量内联在 .app-shell；data-theme 另有用途——assets/themes.css 的暗色/工业风覆盖
  // 写的是 [data-theme=...] 选择器，只改变量不改属性会得到半套主题。
  const themeVars = computed(() => toCssVars(themePresets[theme.value] ?? themePresets.mall))

  function setTheme(next: ThemePresetName) {
    if (themePresets[next]) {
      theme.value = next
    }
  }

  return {
    theme,
    themeVars,
    setTheme,
    presets: themePresets
  }
}
