/**
 * 前后端契约的前端副本：这里的 interface 与 server 侧 DTO 一一对应，改字段必须同步
 * （scripts/check-api-alignment.py 会比对，缺字段/类型不符按缺陷处理）。
 *
 * <p>只放“形状 + 校验常量 + 时间格式”，不放业务逻辑：门店/后台/客服/移动端共用这一份。
 */
export * from './labels'
export * from './validation'
export * from './permissions.generated'
export * from './datetime'
export * from './money'

export interface StaffUser {
  id: string
  username: string
  displayName: string
  role: 'ADMIN' | 'OPERATOR' | 'STORE' | 'CS' | 'SUPPLIER'
  status: string
}

/**
 * 金额一律“分 + 币种”，不用小数：后端以整型存储，跨语言的浮点表示（0.1+0.2）会让对账差几分钱。
 * 展示时按币种小数位换算，统一用 money.ts（禁止页面写死 /100）。
 */
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
  promotionId?: string
  lineType?: 'NORMAL' | 'PROMOTION' | 'GIFT'
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

export interface PosShift {
  id: string
  storeId: string
  cashierName: string
  status: 'OPEN' | 'CLOSED' | 'APPROVED'
  openingFloat: Money
  expectedCash?: Money
  actualCash?: Money
  variance?: Money
  varianceReason?: string
  openedAt: string
  closedAt?: string
}

export interface PosShiftView {
  shift: PosShift
  cashSales: Money
  cashRefunds: Money
  cashIn: Money
  cashOut: Money
  expectedCash: Money
  saleCount: number
}

export interface PosSaleLine {
  id: string
  skuId: string
  title: string
  skuCode: string
  quantity: number
  unitPrice: Money
  lineTotal: Money
  refundedQuantity: number
  refundedAmount: Money
  batchSummary?: string
}

export interface PosTender {
  id: string
  type: 'CASH' | 'WECHAT' | 'ALIPAY' | 'CARD' | 'STORED_VALUE' | 'OTHER'
  amount: Money
  provider: string
  providerTradeNo?: string
  status: 'SUCCESS' | 'FAILED' | 'REVERSED'
  cashTendered?: Money
  change?: Money
  refundedAmount?: Money
  reversalStatus?: string
  paidAt?: string
}

export interface PosSale {
  id: string
  saleNo: string
  storeId: string
  shiftId: string
  memberId?: string
  currency: string
  subtotal: Money
  promotionDiscount: Money
  couponDiscount: Money
  memberDiscount: Money
  pointsDiscount: Money
  payable: Money
  cashTendered: Money
  change: Money
  status: 'DRAFT' | 'COMPLETED' | 'VOIDED' | 'PARTIALLY_REFUNDED' | 'REFUNDED'
  couponId?: string
  couponCode?: string
  pointsRedeemed: number
  pointsEarned: number
  pointsReversed: number
  refunded: Money
  operatorName: string
  createdAt: string
  paidAt?: string
  lines: PosSaleLine[]
  tenders: PosTender[]
}

export interface PosMember {
  id: string
  memberNo: string
  name: string
  phone: string
  tier: string
  birthday?: string
  growthValue: number
  birthdayGiftYear?: number
  points: number
  storedValue: Money
}

export interface PosCoupon {
  id: string
  code: string
  name: string
  discountType: 'FIXED' | 'PERCENT'
  discountValue: number
  minSpend: Money
  maxDiscount?: Money
  currency: string
  status: 'ISSUED' | 'LOCKED' | 'USED' | 'EXPIRED'
  memberId?: string
  validFrom: string
  validTo: string
}

export interface MemberAdminView {
  member: PosMember
  saleCount: number
  totalSpent: Money
  createdAt: string
}

export interface PosMemberView {
  member: PosMember
  coupons: PosCoupon[]
}

export interface PosSaleView {
  sale: PosSale
  paidAmount: Money
  remainingAmount: Money
  member?: PosMember
  coupon?: PosCoupon
  storeName: string
}

export interface PosProductView {
  skuId: string
  skuCode: string
  barcode?: string
  title: string
  price: Money
  availableQuantity: number
  imageUrl?: string
}

export interface PosProviderView {
  code: string
  label: string
  requiresMember: boolean
  mock: boolean
}

export interface PosTenderTotal {
  tenderType: string
  amount: Money
}

export interface PosDailyReport {
  businessDate: string
  storeId: string
  storeName: string
  salesCount: number
  refundCount: number
  grossSales: Money
  discountTotal: Money
  netSales: Money
  refundTotal: Money
  cashTotal: Money
  pointsDiscount: Money
  couponDiscount: Money
  pointsRedeemed: number
  pointsEarned: number
  couponsUsed: number
  cashVariance: Money
  shiftCount: number
  tenderMix: PosTenderTotal[]
  shifts: PosShiftView[]
}

export interface PosReconciliationReport {
  businessDate: string
  storeId: string
  storeName: string
  rows: Array<{
    tenderType: string
    expectedAmount: Money
    actualAmount: Money
    variance: Money
    status: string
  }>
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
  /** 空值表示长期有效；有值表示该版本失效时间 */
  effectiveTo?: string | null
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

// 查询参数只丢 undefined/null：false 与 0 是有效筛选值（includeInactive=false、page=0），
// 用真值判断过滤掉会让筛选条件静默失效。
type QueryValue = string | number | boolean | null | undefined

export class ApiError extends Error {
  /**
   * @param status HTTP 状态码；**0 表示网络层失败**（后端不可达/被拦），此时没有响应体
   * @param body   已解析的 message 或原始响应文本；status=0 时放"给人看的一句话"
   * @param code   稳定错误码（如 NETWORK_UNREACHABLE），上层按它分支，不靠匹配文案
   */
  constructor(
    public readonly status: number,
    public readonly body: string,
    public readonly code?: string
  ) {
    // status=0 时不要把 "API 0:" 拼进用户可见文案
    super(status === 0 ? body : `API ${status}: ${body}`)
    this.name = 'ApiError'
  }
}

/**
 * 极薄的 fetch 封装：**不做重试、不做超时**。浏览器 fetch 没有超时语义，套一层 setTimeout
 * 只会把已成功的请求误判为失败；而非幂等 POST（下单、支付）的重试必须靠 Idempotency-Key
 * （见 storefront 的 checkout 页），不能在这里统一重试。
 *
 * <p>token 只存实例内存：避免 SSR 并发请求之间串号。
 */
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

  post<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'POST',
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  }

  /**
   * multipart 上传。
   *
   * <p>不能复用 post()：它会强制 `Content-Type: application/json`，
   * 手工设 `multipart/form-data` 又会丢 boundary，必须让浏览器自己带。
   */
  postForm<T>(path: string, form: FormData): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'POST',
      body: form,
      headers: {}
    })
  }

  patch<T>(path: string, body?: unknown, headers?: Record<string, string>): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'PATCH',
      headers,
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  }

  delete<T>(path: string): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), { method: 'DELETE' })
  }

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(new URL(path, this.baseUrl).toString(), {
      method: 'PUT',
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  }

  private async request<T>(url: string, init?: RequestInit): Promise<T> {
    // fetch 自身抛错＝网络层失败（后端挂了 / DNS / 证书 / 跨域拦截），此时没有 HTTP 状态码。
    // 必须显式转换：否则上层只能拿到浏览器原文 "Failed to fetch"，既不能按 code 分支，
    // 也没法区分"后端不可用"和"登录态失效"（后者才该跳登录页）。
    let response: Response
    try {
      response = await fetch(url, {
      ...init,
      headers: {
        Accept: 'application/json',
        // FormData 必须让浏览器自动设置带 boundary 的 Content-Type
        ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
        ...(init?.headers ?? {}),
        ...(this.token ? { Authorization: `Bearer ${this.token}` } : {})
      }
      })
    } catch (cause) {
      // 用户文案说人话、可行动；原始异常放进 cause 供控制台/日志排障（§10.2 文案可模糊、日志不能）
      const error = new ApiError(0, '无法连接后端服务，请稍后重试（若持续出现，请检查服务是否在运行）', 'NETWORK_UNREACHABLE')
      ;(error as ApiError & { cause?: unknown }).cause = cause
      throw error
    }
    // 非 2xx 统一抛 ApiError：把 body 里解出的 message/code 提出来，但保留整个 status，
    // 让上层按 401（重新登录）/404（空态）/409（冲突）分别处理；解析失败时退回原始文本，
    // 上游仍能看到网关返回的 HTML 错误页，而不是一句空 message。
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
    // 204 与“200 + 空 body”（如 POST /activate）都不能直接 json()：
    // 否则抛 "Unexpected end of JSON input"，调用方会误判为失败。下面统一按 text 判空。
    if (response.status === 204) {
      return undefined as T
    }
    // 空响应体（例如 POST /activate 返回 200 但无内容）不能直接 json()，
    // 否则会抛 "Unexpected end of JSON input"，让调用方误判为失败。
    const text = await response.text()
    if (text.trim() === '') {
      return undefined as T
    }
    return JSON.parse(text) as T
  }
}

export interface SupplierCredentialView {
  supplierId: string
  code: string
  name: string
  active: boolean
  accessKey?: string
  hasCredential: boolean
  rotatedAt?: string
}


export interface PermissionView {
  code: string
  label: string
  group: string
  sortOrder: number
}

export interface RolePermissionView {
  role: string
  permissions: string[]
}

export interface PermissionMatrixView {
  permissions: PermissionView[]
  roles: RolePermissionView[]
}


export interface AfterSaleLine {
  id: string
  orderLineId: string
  skuId: string
  title: string
  skuCode: string
  quantity: number
}

export interface AfterSaleEvent {
  id: string
  eventType: string
  note?: string
  operatorName?: string
  createdAt: string
}

export interface AfterSaleView {
  id: string
  afterSaleNo: string
  orderId: string
  orderNo: string
  type: 'RETURN' | 'EXCHANGE'
  status: 'REQUESTED' | 'APPROVED' | 'REJECTED' | 'RECEIVED' | 'REFUNDED' | 'CANCELLED'
  reason: string
  description?: string
  contactPhone: string
  refundAmount: Money
  adminNote?: string
  createdAt: string
  approvedAt?: string
  receivedAt?: string
  refundedAt?: string
  rejectedAt?: string
  lines: AfterSaleLine[]
  events: AfterSaleEvent[]
}


export interface MemberLevel {
  code: string
  name: string
  discountRate: number
  pointsMultiplier: number
  freeShipping: boolean
  birthdayPoints: number
  minGrowth: number
  sortOrder: number
}


export type PromotionType = 'FLASH_SALE' | 'BUY_GIFT' | 'GROUP_BUY'
export type PromotionStatus = 'DRAFT' | 'ACTIVE' | 'ENDED'

export interface PromotionView {
  id: string
  code: string
  type: PromotionType
  title: string
  description?: string
  status: PromotionStatus
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
  priority: number
}

export interface CreatePromotionInput {
  code: string
  type: PromotionType
  title: string
  description?: string
  startsAt: string
  endsAt: string
  skuId: string
  promoPriceAmount?: number
  giftSkuId?: string
  buyQuantity?: number
  giftQuantity?: number
  maxGiftPerOrder?: number
  stockLimit?: number
  perUserLimit?: number
  groupSize?: number
  groupDurationHours?: number
  priority?: number
}

export interface GroupBuyMemberView {
  maskedPhone: string
  role: 'CREATOR' | 'MEMBER'
  paid: boolean
  joinedAt: string
}

export interface GroupBuyView {
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
  createdAt: string
  completedAt?: string
  members: GroupBuyMemberView[]
}

export interface CreateGroupBuyInput {
  promotionId: string
  phone: string
}

export { formatDateTime, formatDate, ADMIN_TIME_ZONE } from './datetime'
