import { ref, shallowRef } from 'vue'
import { ApiError } from '@fullmall/api-client'

/** 统一的接口错误文案：优先用后端 message，其次 HTTP 状态，最后兜底。 */
export function apiErrorMessage(cause: unknown, fallback = '请求失败'): string {
  if (cause instanceof ApiError) {
    return cause.message || `HTTP ${cause.status}`
  }
  if (cause instanceof Error) {
    return cause.message || fallback
  }
  return fallback
}

export interface AsyncState<T> {
  data: ReturnType<typeof shallowRef<T | null>>
  loading: ReturnType<typeof ref<boolean>>
  error: ReturnType<typeof ref<string>>
  /** 是否至少成功加载过一次：用于区分"还没加载"与"加载了但为空" */
  loaded: ReturnType<typeof ref<boolean>>
  run: () => Promise<T | null>
}

/**
 * 异步数据统一状态：loading / error / data 三态 + 过期响应保护。
 *
 * 为什么需要（设计审查 2026-10-28）：
 * - 过去各页面自己写 try/catch + ElMessage，失败时**不清理旧数据**，
 *   于是"切到日语失败"后表单里留着英文内容，选择框与实际值不一致；
 * - 失败只剩一条 3 秒的 toast，页面本身看不出来；
 * - 快速切换（语言、分页、搜索）时先发的请求可能后到，覆盖新结果。
 *
 * 使用约定：失败时 data 置空并保留 error，由页面显示可重试的错误块；
 * 不允许"catch 后当作空数据"。
 */
export function useAsyncState<T>(loader: () => Promise<T>): AsyncState<T> {
  const data = shallowRef<T | null>(null)
  const loading = ref(false)
  const error = ref('')
  const loaded = ref(false)
  let token = 0

  async function run(): Promise<T | null> {
    const current = ++token
    loading.value = true
    error.value = ''
    try {
      const result = await loader()
      if (current !== token) return null
      data.value = result
      loaded.value = true
      return result
    } catch (cause) {
      if (current !== token) return null
      data.value = null
      loaded.value = false
      error.value = apiErrorMessage(cause)
      return null
    } finally {
      if (current === token) loading.value = false
    }
  }

  return { data, loading, error, loaded, run }
}
