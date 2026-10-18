import type { Money } from '~/types'

export interface CartLine {
  productId: string
  skuId: string
  skuCode: string
  title: string
  price: Money
  quantity: number
}

export function useCart() {
  const cart = useState<CartLine[]>('cart', () => [])

  if (import.meta.client) {
    const raw = localStorage.getItem('fullmall.cart')
    if (raw && cart.value.length === 0) {
      try {
        cart.value = JSON.parse(raw) as CartLine[]
      } catch {
        localStorage.removeItem('fullmall.cart')
        throw new Error('购物车数据损坏，已清除本地购物车')
      }
    }
    watch(cart, value => {
      localStorage.setItem('fullmall.cart', JSON.stringify(value))
    }, { deep: true })
  }

  const count = computed(() => cart.value.reduce((sum, line) => sum + line.quantity, 0))
  const total = computed<Money>(() => {
    const currency = cart.value[0]?.price.currency ?? 'CNY'
    return {
      amount: cart.value.reduce((sum, line) => sum + line.price.amount * line.quantity, 0),
      currency
    }
  })

  function add(line: CartLine) {
    const existing = cart.value.find(item => item.skuId === line.skuId)
    if (existing) {
      existing.quantity += line.quantity
      return
    }
    cart.value.push(line)
  }

  function clear() {
    cart.value = []
  }

  function updateQuantity(skuId: string, quantity: number) {
    const line = cart.value.find(item => item.skuId === skuId)
    if (!line) {
      return
    }
    if (quantity <= 0) {
      remove(skuId)
      return
    }
    line.quantity = Math.min(quantity, 99)
  }

  function remove(skuId: string) {
    cart.value = cart.value.filter(item => item.skuId !== skuId)
  }

  return { cart, count, total, add, clear, updateQuantity, remove }
}