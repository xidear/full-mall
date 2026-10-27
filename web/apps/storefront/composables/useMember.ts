/**
 * 会员登录态：cookie 供 SSR 读取，localStorage 兼容历史数据。
 * 订单详情页在服务端渲染时只能读到 cookie，因此登录成功后必须同时写 cookie。
 */
export function useMember() {
  const cookie = useCookie<string | null>('fullmall_member_token', {
    sameSite: 'lax',
    maxAge: 60 * 60 * 24 * 30
  })
  const token = useState<string>('member-token', () => cookie.value ?? '')

  if (import.meta.client) {
    onMounted(() => {
      if (!token.value) {
        token.value = localStorage.getItem('fullmall.memberToken') ?? ''
      }
    })
  }

  function setToken(value: string) {
    token.value = value
    cookie.value = value
    if (import.meta.client) {
      localStorage.setItem('fullmall.memberToken', value)
    }
  }

  function clearToken() {
    token.value = ''
    cookie.value = null
    if (import.meta.client) {
      localStorage.removeItem('fullmall.memberToken')
    }
  }

  return { token, setToken, clearToken }
}
