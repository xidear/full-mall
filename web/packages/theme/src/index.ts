export type ThemePresetName = 'mall' | 'industrial' | 'fresh' | 'minimal'
export type CardStyle = 'soft' | 'sharp' | 'outline'
export type ProductLayout = 'grid' | 'list'

export interface ThemeColors {
  brand: string
  brandDark: string
  accent: string
  background: string
  surface: string
  surfaceMuted: string
  text: string
  muted: string
  line: string
}

export interface ThemeConfig {
  preset: ThemePresetName
  label: string
  colors: ThemeColors
  radius: number
  fontScale: number
  cardStyle: CardStyle
  productLayout: ProductLayout
  showBanner: boolean
  bannerText: string
  fontFamily: string
}

const systemFont = '"PingFang SC", "Microsoft YaHei", "Noto Sans SC", system-ui, sans-serif'
const monoFont = '"IBM Plex Mono", "Roboto Mono", "SFMono-Regular", ui-monospace, monospace'

export const themePresets: Record<ThemePresetName, ThemeConfig> = {
  mall: {
    preset: 'mall',
    label: '商城模板',
    colors: {
      brand: '#e1251b',
      brandDark: '#b51f17',
      accent: '#ff7a00',
      background: '#f5f5f5',
      surface: '#ffffff',
      surfaceMuted: '#f0f0f0',
      text: '#222222',
      muted: '#666666',
      line: '#e5e5e5'
    },
    radius: 8,
    fontScale: 1,
    cardStyle: 'soft',
    productLayout: 'grid',
    showBanner: true,
    bannerText: '好物直达，门店自提更快',
    fontFamily: systemFont
  },
  industrial: {
    preset: 'industrial',
    label: '机械模板',
    colors: {
      brand: '#2f6f8f',
      brandDark: '#1c4659',
      accent: '#d99a2b',
      background: '#10161b',
      surface: '#182129',
      surfaceMuted: '#22303a',
      text: '#e8edf0',
      muted: '#93a4ae',
      line: '#30414c'
    },
    radius: 4,
    fontScale: 0.96,
    cardStyle: 'sharp',
    productLayout: 'list',
    showBanner: true,
    bannerText: '工业级选型 · 稳定交付',
    fontFamily: monoFont
  },
  fresh: {
    preset: 'fresh',
    label: '清新模板',
    colors: {
      brand: '#1f7a6b',
      brandDark: '#145a4f',
      accent: '#e08b4c',
      background: '#f3f8f5',
      surface: '#ffffff',
      surfaceMuted: '#e3f0e9',
      text: '#1d2a25',
      muted: '#63756d',
      line: '#cfe0d7'
    },
    radius: 14,
    fontScale: 1,
    cardStyle: 'soft',
    productLayout: 'grid',
    showBanner: true,
    bannerText: '今天也要吃得新鲜',
    fontFamily: systemFont
  },
  minimal: {
    preset: 'minimal',
    label: '极简模板',
    colors: {
      brand: '#111111',
      brandDark: '#000000',
      accent: '#d04a2b',
      background: '#fafafa',
      surface: '#ffffff',
      surfaceMuted: '#f0f0f0',
      text: '#111111',
      muted: '#666666',
      line: '#dddddd'
    },
    radius: 0,
    fontScale: 1,
    cardStyle: 'outline',
    productLayout: 'grid',
    showBanner: true,
    bannerText: '少一点，刚刚好',
    fontFamily: systemFont
  }
}

export function clonePreset(name: ThemePresetName): ThemeConfig {
  return JSON.parse(JSON.stringify(themePresets[name])) as ThemeConfig
}

export function toCssVars(theme: ThemeConfig): Record<string, string> {
  return {
    '--brand': theme.colors.brand,
    '--brand-dark': theme.colors.brandDark,
    '--accent': theme.colors.accent,
    '--bg': theme.colors.background,
    '--surface': theme.colors.surface,
    '--surface-muted': theme.colors.surfaceMuted,
    '--text': theme.colors.text,
    '--muted': theme.colors.muted,
    '--line': theme.colors.line,
    '--radius': `${theme.radius}px`,
    '--font-scale': String(theme.fontScale),
    '--font-family': theme.fontFamily
  }
}

export function themeNames(): ThemePresetName[] {
  return Object.keys(themePresets) as ThemePresetName[]
}