export function useOrders() {
  const orderIds = useState<string[]>('orderIds', () => [])

  if (import.meta.client) {
    onMounted(() => {
      if (orderIds.value.length > 0) return
      const raw = localStorage.getItem('fullmall.orderIds')
      if (raw) {
        orderIds.value = JSON.parse(raw) as string[]
      }
    })
  }

  function remember(orderId: string) {
    if (!orderIds.value.includes(orderId)) {
      orderIds.value = [orderId, ...orderIds.value].slice(0, 50)
      if (import.meta.client) {
        localStorage.setItem('fullmall.orderIds', JSON.stringify(orderIds.value))
      }
    }
  }

  return { orderIds, remember }
}