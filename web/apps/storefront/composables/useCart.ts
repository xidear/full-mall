import type { Money } from '~/types'
import { VALIDATION } from '@fullmall/api-client'

export interface CartLine {
  productId: string
  skuId: string
  skuCode: string
  title: string
  imageUrl?: string
  price: Money
  quantity: number
}

const STORAGE_KEY = 'fullmall.cart'

/** useState（SSR 与客户端共享，首屏件数才一致）+ localStorage，换浏览器即丢。
 *  服务端没有购物车实体，价格在结算时由后端重算，total 只是预估，不能拿它对账。 */
export function useCart() {
  const cart = useState<CartLine[]>('cart', () => [])

  if (import.meta.client) {
    // 不能在 setup 阶段直接改 cart：SSR 渲染数量 0、客户端首帧变成 N，会触发 hydration mismatch
    onMounted(() => {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (raw && cart.value.length === 0) {
        // 不能静默当空车：先清坏数据再抛，否则顾客只会发现“少了几件商品”却没有任何失败信号。
        try {
          cart.value = JSON.parse(raw) as CartLine[]
        } catch {
          localStorage.removeItem(STORAGE_KEY)
          throw new Error('购物车数据损坏，已清除本地购物车')
        }
      }
    })
  }

  function persist() {
    if (import.meta.client) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cart.value))
    }
  }

  const count = computed(() => cart.value.reduce((sum, line) => sum + line.quantity, 0))
  // 合计按整数分累加（见 utils/money.ts）；币种取第一行：混币种没有汇率能力，是已知未覆盖的边界。
  const total = computed<Money>(() => {
    const currency = cart.value[0]?.price.currency ?? 'CNY'
    return {
      amount: cart.value.reduce((sum, line) => sum + line.price.amount * line.quantity, 0),
      currency
    }
  })

  function add(line: CartLine) {
    // K7 埋点：加购行为（失败不影响加购）
    track('ADD_TO_CART', { skuId: line.skuId, quantity: line.quantity })
    const existing = cart.value.find(item => item.skuId === line.skuId)
    if (existing) {
      existing.quantity += line.quantity
    } else {
      cart.value.push(line)
    }
    persist()
  }

  function clear() {
    cart.value = []
    persist()
  }

  // 上限来自 VALIDATION.quantityMax（生成自 validation.schema.json）：这里只是快速反馈，后端才是权威。
  function updateQuantity(skuId: string, quantity: number) {
    const line = cart.value.find(item => item.skuId === skuId)
    if (!line) {
      return
    }
    if (quantity <= 0) {
      remove(skuId)
      return
    }
    line.quantity = Math.min(quantity, VALIDATION.quantityMax)
    persist()
  }

  function remove(skuId: string) {
    cart.value = cart.value.filter(item => item.skuId !== skuId)
    persist()
  }

  return { cart, count, total, add, clear, updateQuantity, remove }
}