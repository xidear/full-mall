import type { Money } from '~/types'

export function formatMoney(money: Money | null | undefined): string {
  if (!money) {
    return '-'
  }
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency: money.currency
  }).format(money.amount / 100)
}