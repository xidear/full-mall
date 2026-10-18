const orderStatus: Record<string, string> = {
  PENDING_PAYMENT: '待付款',
  PAID: '已付款',
  ALLOCATED: '已分配履约',
  PARTIALLY_FULFILLED: '部分完成',
  FULFILLED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款'
}

const fulfillmentStatus: Record<string, string> = {
  PENDING: '待处理',
  ALLOCATED: '已分配',
  PICKING: '拣货中',
  PACKED: '已打包',
  SHIPPED: '已发货',
  READY_FOR_PICKUP: '待自提',
  COMPLETED: '已完成',
  CANCELLED: '已取消'
}

const fulfillmentType: Record<string, string> = {
  WAREHOUSE_SHIP: '仓库发货',
  STORE_SHIP: '门店发货',
  SUPPLIER_DROPSHIP: '供应商代发',
  STORE_PICKUP: '门店自提'
}

const pickTaskStatus: Record<string, string> = {
  PENDING: '待拣货',
  PICKING: '拣货中',
  PICKED: '已拣货',
  CANCELLED: '已取消'
}

const pickupStatus: Record<string, string> = {
  PENDING: '待备货',
  READY: '待提货',
  PARTIALLY_USED: '部分核销',
  COMPLETED: '已核销',
  EXPIRED: '已过期',
  CANCELLED: '已取消'
}

const paymentStatus: Record<string, string> = {
  PENDING: '待支付',
  AUTHORIZED: '已授权',
  PAID: '已支付',
  FAILED: '支付失败',
  REFUNDED: '已退款',
  PARTIALLY_REFUNDED: '部分退款'
}

const productStatus: Record<string, string> = {
  DRAFT: '草稿',
  ACTIVE: '在售',
  ARCHIVED: '已归档'
}

const inventoryStatus: Record<string, string> = {
  AVAILABLE: '可用',
  QUARANTINE: '隔离',
  NEAR_EXPIRY: '临期',
  EXPIRED: '已过期',
  BLOCKED: '冻结'
}

const locationType: Record<string, string> = {
  WAREHOUSE: '仓库',
  STORE: '门店',
  SUPPLIER: '供应商'
}

const inventoryTracking: Record<string, string> = {
  VARIANT: '按规格（SKU）',
  PRODUCT: '按商品（SPU）'
}

const pickingStrategy: Record<string, string> = {
  FEFO: '先到期先出',
  FIFO: '先进先出',
  NONE: '不按批次'
}

const nearExpiryAction: Record<string, string> = {
  NORMAL: '正常销售',
  DISCOUNT: '折扣清货',
  DELIST: '下架',
  WRITE_OFF: '报损'
}

const fulfillmentMode: Record<string, string> = {
  SHIP: '快递配送',
  PICKUP: '门店自提'
}

const role: Record<string, string> = {
  ADMIN: '超级管理员',
  OPERATOR: '运营人员',
  STORE: '门店人员',
  CS: '客服人员',
  SUPPLIER: '供应商'
}

function translate(map: Record<string, string>, key: string | undefined | null): string {
  if (!key) return '-'
  return map[key] ?? key
}

export const orderStatusLabel = (key?: string | null) => translate(orderStatus, key)
export const fulfillmentStatusLabel = (key?: string | null) => translate(fulfillmentStatus, key)
export const fulfillmentTypeLabel = (key?: string | null) => translate(fulfillmentType, key)
export const pickTaskStatusLabel = (key?: string | null) => translate(pickTaskStatus, key)
export const pickupStatusLabel = (key?: string | null) => translate(pickupStatus, key)
export const paymentStatusLabel = (key?: string | null) => translate(paymentStatus, key)
export const productStatusLabel = (key?: string | null) => translate(productStatus, key)
export const inventoryStatusLabel = (key?: string | null) => translate(inventoryStatus, key)
export const locationTypeLabel = (key?: string | null) => translate(locationType, key)
export const inventoryTrackingLabel = (key?: string | null) => translate(inventoryTracking, key)
export const pickingStrategyLabel = (key?: string | null) => translate(pickingStrategy, key)
export const nearExpiryActionLabel = (key?: string | null) => translate(nearExpiryAction, key)
export const fulfillmentModeLabel = (key?: string | null) => translate(fulfillmentMode, key)
export const roleLabel = (key?: string | null) => translate(role, key)