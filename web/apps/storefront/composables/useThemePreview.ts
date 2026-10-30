import { ref } from 'vue'

/**
 * 主题草稿预览桥（仅客户端）。
 *
 * <p>后台「主题工作室」用 iframe 嵌入**真实商城首页**做预览，而不是手搓一个示意图：
 * 示意图永远和真实页面有差异（用户已经指出"说是 PC 预览，其实是放大的手机版"）。
 *
 * <p>通道：父窗口 postMessage `{ type: 'fullmall:theme-preview', vars: {...} }`，
 * 这里把 vars 覆盖到 .app-shell 的内联样式上——**不写 cookie、不进服务端**，
 * 运营改了不保存也不会污染线上配色。
 */
export interface ThemePreviewMessage {
  type: 'fullmall:theme-preview'
  vars: Record<string, string> | null
}

export function useThemePreviewBridge() {
  const overrideVars = ref<Record<string, string> | null>(null)

  if (import.meta.client) {
    window.addEventListener('message', (event: MessageEvent) => {
      const data = event.data as ThemePreviewMessage | null
      if (!data || data.type !== 'fullmall:theme-preview') {
        return
      }
      overrideVars.value = data.vars
    })

    // 通知父窗口"可以推草稿了"（父窗口可能比我们晚就绪）
    if (window.parent !== window) {
      window.parent.postMessage({ type: 'fullmall:theme-preview-ready' }, '*')
    }
  }

  return { overrideVars }
}
