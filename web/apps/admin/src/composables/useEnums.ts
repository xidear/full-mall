import { ref } from 'vue'
import { api } from '../api'

/**
 * 后台枚举字典（码 → 中文），来自后端 /admin/v1/meta/enums。
 *
 * <p>表格里直接渲染 `PRODUCT`/`IMAGE`/`SUCCEEDED` 这类码，运营看不懂；各页面各写一份
 * 中文映射又会随枚举扩展走样。这里只加载一次（模块级缓存），页面用 enumLabel(group, code)。
 *
 * <p>未知码回退显示原始码——宁可露出内部码，也不要把真实值藏成空白，便于发现后端漏登记。
 */
interface EnumOption { code: string; label: string }

const groups = ref<Record<string, EnumOption[]>>({})
const loading = ref(false)
const error = ref('')
let requested = false

async function ensureLoaded() {
  if (requested) return
  requested = true
  loading.value = true
  try {
    groups.value = await api.get<Record<string, EnumOption[]>>('/admin/v1/meta/enums')
  } catch (cause) {
    error.value = '枚举字典加载失败：' + (cause instanceof Error ? cause.message : '')
  } finally {
    loading.value = false
  }
}

export function useEnums() {
  void ensureLoaded()

  function options(group: string): EnumOption[] {
    return groups.value[group] ?? []
  }

  function enumLabel(group: string, code?: string | null): string {
    if (code == null || code === '') return '—'
    return options(group).find(item => item.code === code)?.label ?? code
  }

  return { groups, loading, error, options, enumLabel }
}
