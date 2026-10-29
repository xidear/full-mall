/**
 * 面向顾客的错误文案。
 *
 * <p>ofetch 的 `error.message` 会把请求方法、路径和状态码直接拼进来
 * （如 `[POST] "/api/v1/member/auth/verify": 400`），这不是给顾客看的内容。
 * 这里优先使用后端返回的业务文案，其次按状态码给出人话，绝不回显请求路径。
 */
interface ApiErrorLike {
  data?: { message?: string; code?: string }
  statusCode?: number
  status?: number
  response?: { status?: number; _data?: { message?: string; code?: string } }
}

const codeMessages: Record<string, string> = {
  LEGAL_DOCUMENT_NOT_FOUND: '该条款暂未发布，请稍后再试',
  MEMBER_REQUIRED: '请先登录后再继续',
  OTP_MISMATCH: '验证码不正确',
  OTP_EXPIRED: '验证码已过期，请重新获取',
  OTP_NOT_REQUESTED: '请先获取验证码',
  OTP_INVALID_FORMAT: '验证码为 6 位数字',
  OTP_REQUEST_TOO_FREQUENT: '验证码请求过于频繁，请稍后再试',
  INVALID_PHONE: '手机号格式不正确'
}

const statusMessages: Record<number, string> = {
  400: '提交的信息有误，请检查后重试',
  401: '登录已过期，请重新登录',
  403: '没有权限执行该操作',
  404: '没有找到相关内容',
  409: '操作冲突，请刷新后重试',
  422: '提交的信息不完整，请检查后重试'
}

export function apiErrorMessage(cause: unknown, fallback = '操作失败，请稍后重试'): string {
  if (cause && typeof cause === 'object') {
    const error = cause as ApiErrorLike
    const data = error.data ?? error.response?._data
    if (data?.code && codeMessages[data.code]) {
      return codeMessages[data.code]
    }
    const message = data?.message?.trim()
    if (message && message.length <= 120 && !message.includes('/api/') && !message.includes('http')) {
      return message
    }
    const status = error.statusCode ?? error.status ?? error.response?.status
    if (status && statusMessages[status]) {
      return statusMessages[status]
    }
    if (status && status >= 500) {
      return '服务暂时不可用，请稍后重试'
    }
  }
  return fallback
}
