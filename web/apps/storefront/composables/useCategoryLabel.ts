/** 分类名称按当前语言取译名；后端分类表没有多语言列，缺失时回退分类自带的 name。 */
export function useCategoryLabel() {
  const { t } = useLocale()
  return (category: { code: string; name: string }) => {
    const key = `category.${category.code}`
    const translated = t(key)
    return translated === key ? category.name : translated
  }
}
