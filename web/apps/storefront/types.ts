export interface Category {
  id: string
  parentId?: string
  code: string
  name: string
}

export interface Money {
  amount: number
  currency: string
}

export interface Sku {
  id: string
  skuCode: string
  barcode?: string
  price: Money
  compareAtPrice?: Money
  options: Record<string, string>
  imageUrl?: string
  shelfLifePolicy?: {
    batchTracking?: boolean
    pickingStrategy?: 'FEFO' | 'FIFO' | 'NONE'
    minRemainingShelfLifeDays?: number
    nearExpiryThresholdDays?: number
    nearExpiryAction?: string
    autoBlockExpired?: boolean
  }
}

export interface Product {
  id: string
  title: string
  description?: string
  categoryId?: string
  status: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  inventoryTrackingLevel: 'VARIANT' | 'PRODUCT'
  shelfLifeManaged: boolean
  skus: Sku[]
}

export interface ProductPage {
  items: Product[]
  page: number
  size: number
  total: number
}

export interface Store {
  id: string
  name: string
  address: string
  latitude?: number
  longitude?: number
  pickupEnabled: boolean
  phone?: string
  openingHours?: string
  distanceMeters?: number
}

export interface Order {
  id: string
  orderNo: string
  status: string
  currency: string
  subtotal: Money
  shippingFee: Money
  discount: Money
  total: Money
  fulfillmentMode: 'SHIP' | 'PICKUP'
  storeId?: string
  contactName: string
  contactPhone: string
  createdAt: string
  lines: OrderLine[]
}

export interface OrderLine {
  id: string
  skuId: string
  title: string
  skuCode: string
  quantity: number
  unitPrice: Money
  fulfillmentType: string
}

export interface Fulfillment {
  id: string
  orderId: string
  type: string
  status: string
  sourceId?: string
  carrierCode?: string
  trackingNo?: string
  pickupCode?: string
  pickupExpiresAt?: string
  lines: Array<{ skuId: string; quantity: number }>
}

export interface LegalDocument {
  id: string
  type: string
  version: string
  locale: string
  market: string
  title: string
  content: string
  contentHash: string
  effectiveFrom: string
}

export interface AfterSaleLine {
  id: string
  orderLineId: string
  skuId: string
  title: string
  skuCode: string
  quantity: number
}

export interface AfterSale {
  id: string
  afterSaleNo: string
  orderId: string
  orderNo: string
  type: 'RETURN' | 'EXCHANGE'
  status: string
  reason: string
  description?: string
  contactPhone: string
  refundAmount: Money
  adminNote?: string
  createdAt: string
  lines: AfterSaleLine[]
}


export type PromotionType = 'FLASH_SALE' | 'BUY_GIFT' | 'GROUP_BUY'

export interface Promotion {
  id: string
  code: string
  type: PromotionType
  title: string
  description?: string
  status: string
  startsAt: string
  endsAt: string
  active: boolean
  soldOut: boolean
  skuId: string
  skuCode: string
  productId: string
  productTitle: string
  originalPrice: Money
  promoPrice?: Money
  giftSkuId?: string
  giftSkuCode?: string
  giftTitle?: string
  buyQuantity?: number
  giftQuantity?: number
  maxGiftPerOrder?: number
  stockLimit?: number
  soldQuantity: number
  remainingStock?: number
  perUserLimit?: number
  groupSize?: number
  groupDurationHours?: number
}

export interface GroupBuyMember {
  maskedPhone: string
  role: 'CREATOR' | 'MEMBER'
  paid: boolean
  joinedAt: string
}

export interface GroupBuy {
  id: string
  code: string
  promotionId: string
  skuId: string
  skuCode: string
  productTitle: string
  price: Money
  groupSize: number
  joinedCount: number
  paidCount: number
  remainingSlots: number
  status: 'FORMING' | 'SUCCESS' | 'FAILED'
  expiresAt: string
  members: GroupBuyMember[]
}
