import type { Money } from '~/types'

export interface CartLine {
  productId: string
  skuId: string
  skuCode: string
  title: string
  price: Money
  quantity: number
}

const STORAGE_KEY = 'fullmall.cart'

export function useCart() {
  const cart = useState<CartLine[]>('cart', () => [])

  if (import.meta.client) {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw && cart.value.length === 0) {
      try {
        cart.value = JSON.parse(raw) as CartLine[]
      } catch {
        localStorage.removeItem(STORAGE_KEY)
        throw new Error('购物车数据损坏，已清除本地购物车')
      }
    }
  }

  function persist() {
    if (import.meta.client) {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cart.value))
    }
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
    } else {
      cart.value.push(line)
    }
    persist()
  }

  function clear() {
    cart.value = []
    persist()
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
    persist()
  }

  function remove(skuId: string) {
    cart.value = cart.value.filter(item => item.skuId !== skuId)
    persist()
  }

  return { cart, count, total, add, clear, updateQuantity, remove }
}