/**
 * 埋点（K7）：只上报行为，不阻断交互。
 * 失败**不重试、不弹窗**，但会打 console.warn，便于发现"埋点全挂了"。
 */
type EventType = 'VIEW' | 'ADD_TO_CART' | 'SEARCH' | 'CLICK' | 'PURCHASE'

interface EventPayload {
  skuId?: string
  keyword?: string
  memberId?: string
  [key: string]: unknown
}

export function track(eventType: EventType, payload: EventPayload = {}) {
  if (!import.meta.client) {
    return
  }
  const config = useRuntimeConfig()
  const base = config.public.apiBase
  const anonymousId = localStorage.getItem('fullmall-anon') ?? (() => {
    const value = crypto.randomUUID()
    localStorage.setItem('fullmall-anon', value)
    return value
  })()

  void $fetch(`${base}/api/v1/events`, {
    method: 'POST',
    body: {
      eventType,
      anonymousId,
      skuId: payload.skuId,
      keyword: payload.keyword,
      memberId: payload.memberId,
      payload
    }
  }).catch((error: unknown) => {
    // 埋点失败不影响业务；用 warn 暴露而不是静默吞掉
    console.warn('[tracking] 上报失败', eventType, error)
  })
}
