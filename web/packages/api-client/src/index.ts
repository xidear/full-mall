export * from './labels'

export interface StaffUser {
  id: string
  username: string
  displayName: string
  role: 'ADMIN' | 'OPERATOR' | 'STORE' | 'CS' | 'SUPPLIER'
  status: string
}

export interface Money {
  amount: number
  currency: string
}

export interface Sku {
  id: string
  productId: string
  skuCode: string
  barcode?: string
  price: Money
  compareAtPrice?: Money
  options: Record<string, string>
  imageUrl?: string
  shelfLifePolicy?: ShelfLifePolicy
}

export interface ShelfLifePolicy {
  batchTracking: boolean
  pickingStrategy: 'FEFO' | 'FIFO' | 'NONE'
  minRemainingShelfLifeDays: number
  nearExpiryThresholdDays: number
  nearExpiryAction: 'NORMAL' | 'DISCOUNT' | 'DELIST' | 'WRITE_OFF'
  autoBlockExpired: boolean
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

export interface InventoryBatch {
  id: string
  skuId: string
  locationType: 'WAREHOUSE' | 'STORE' | 'SUPPLIER'
  locationId: string
  batchNo: string
  productionDate?: string
  expiryDate?: string
  quantity: number
  reservedQuantity: number
  status: 'AVAILABLE' | 'QUARANTINE' | 'NEAR_EXPIRY' | 'EXPIRED' | 'BLOCKED'
  unitCost?: Money
  receivedAt: string
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

export interface Order {
  id: string
  orderNo: string
  status: string
  currency: string
  subtotal: Money
  shippingFee: Money
  discount: Money
  total: Money
  contactName: string
  contactPhone: string
  contactEmail?: string
  fulfillmentMode: 'SHIP' | 'PICKUP'
  storeId?: string
  createdAt: string
  lines: OrderLine[]
}

export interface OrderSummary {
  id: string
  orderNo: string
  status: string
  total: Money
  fulfillmentMode: string
  contactName: string
  createdAt: string
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

export interface PickTaskLine {
  id: string
  pickTaskId: string
  skuId: string
  title?: string
  quantity: number
  suggestedBatchId?: string
  suggestedBatchNo?: string
  suggestedExpiryDate?: string
  pickedQuantity: number
}

export interface PickTask {
  id: string
  fulfillmentOrderId: string
  storeId?: string
  status: string
  lines: PickTaskLine[]
}

export interface Pickup {
  id: string
  fulfillmentOrderId: string
  storeId: string
  pickupCode: string
  status: string
  expiresAt: string
  completedAt?: string
  totalUses: number
  usedCount: number
}

export interface PickupDetail {
  pickupCode: string
  status: string
  storeName: string
  orderNo: string
  customerName: string
  customerPhone: string
  total: Money
  totalUses: number
  usedCount: number
  remainingUses: number
  expiresAt: string
  items: Array<{ title: string; quantity: number }>
}

export interface AnalyticsOverview {
  gmv: Money
  orders: number
  paidOrders: number
  aov?: Money
  pickupRate?: number
  fulfillmentOnTimeRate?: number
  nearExpiryValue: Money
  botContainmentRate?: number
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

export interface ProductUpsertRequest {
  title: string
  description?: string
  categoryId?: string
  status?: 'DRAFT' | 'ACTIVE' | 'ARCHIVED'
  inventoryTrackingLevel: 'VARIANT' | 'PRODUCT'
  shelfLifeManaged: boolean
  skus: Array<{
    skuCode: string
    barcode?: string
    priceAmount: number
    currency: string
    compareAtAmount?: number
    options: Record<string, string>
    imageUrl?: string
    shelfLifePolicy?: ShelfLifePolicy
  }>
}

type QueryValue = string | number | boolean | null | undefined

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly body: string
  ) {
    super(`API ${status}: ${body}`)
    this.name = 'ApiError'
  }
}

export class ApiClient {
  private token?: string

  constructor(private readonly baseUrl: string) {}

  setToken(token: string | null | undefined) {
    this.token = token || undefined
  }

  get<T>(path: string, query?: Record<string, QueryValue>): Promise<T> {
    const url = new URL(path, this.baseUrl)
    if (query) {
      for (const [key, value] of Object.entries(query)) {
        if (value !== undefined && value !== null) {
          url.searchParams.set(key, String(value))
        }
      }
    }
    return this.request<T>(url.toString())
  }

  post<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'POST',
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  }

  patch<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'PATCH',
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'PUT',
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  }

  private async request<T>(url: string, init?: RequestInit): Promise<T> {
    const response = await fetch(url, {
      ...init,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...(init?.headers ?? {}),
        ...(this.token ? { Authorization: `Bearer ${this.token}` } : {})
      }
    })
    if (!response.ok) {
      const body = await response.text()
      let message = body
      try {
        const parsed = JSON.parse(body) as { message?: string; code?: string }
        message = parsed.message || parsed.code || body
      } catch {
        // Keep the original body for non-JSON upstream errors.
      }
      throw new ApiError(response.status, message)
    }
    if (response.status === 204) {
      return undefined as T
    }
    return response.json() as Promise<T>
  }
}