import {
  themePresets,
  toCssVars,
  type ThemePresetName
} from '@fullmall/theme'

export function useTheme() {
  const config = useRuntimeConfig()
  const initial = (config.public.theme as ThemePresetName) || 'mall'
  const theme = useState<ThemePresetName>('theme', () => themePresets[initial] ? initial : 'mall')

  if (import.meta.client) {
    const queryTheme = new URLSearchParams(location.search).get('theme') as ThemePresetName | null
    const storedTheme = localStorage.getItem('fullmall.theme') as ThemePresetName | null
    if (queryTheme && themePresets[queryTheme]) {
      theme.value = queryTheme
    } else if (storedTheme && themePresets[storedTheme]) {
      theme.value = storedTheme
    }
    watch(theme, value => {
      localStorage.setItem('fullmall.theme', value)
    })
  }

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