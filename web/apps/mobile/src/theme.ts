import { computed, ref } from 'vue'
import {
  themeNames,
  themePresets,
  toCssVars,
  type ThemeConfig,
  type ThemePresetName
} from '@fullmall/theme'

export function useMobileTheme() {
  const theme = ref<ThemePresetName>('mall')
  const remoteConfig = ref<ThemeConfig | null>(null)
  const vars = computed(() => toCssVars(remoteConfig.value ?? themePresets[theme.value]))
  const presets = themeNames().map(name => themePresets[name])

  function setTheme(name: ThemePresetName) {
    if (themePresets[name]) {
      remoteConfig.value = null
      theme.value = name
    }
  }

  function loadRemote() {
    uni.request({
      url: `${import.meta.env.VITE_API_BASE || 'http://localhost:8080'}/api/v1/theme`,
      data: { channel: 'MINI', market: 'CN' },
      success: response => {
        const payload = response.data as { config?: ThemeConfig | null }
        if (payload.config) {
          remoteConfig.value = payload.config
        }
      }
    })
  }

  return { theme, vars, presets, setTheme, loadRemote }
}