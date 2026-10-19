<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'

interface SupplierOrderLine {
  skuId: string
  title: string
  skuCode: string
  supplierSkuCode?: string
  quantity: number
  unitPrice: { amount: number; currency: string }
}

interface SupplierOrder {
  fulfillmentId: string
  orderId: string
  orderNo: string
  status: string
  createdAt: string
  acceptedAt?: string
  carrierCode?: string
  trackingNo?: string
  contactName: string
  contactPhone: string
  addressJson?: string
  total: { amount: number; currency: string }
  note?: string
  lines: SupplierOrderLine[]
}

interface FeedRow {
  supplierSkuCode: string
  availableQuantity: number
  priceAmount: number
}

const API_BASE = import.meta.env.VITE_API_BASE || ''
const accessKey = ref(sessionStorage.getItem('fullmall.supplier.key') || '')
const secret = ref(sessionStorage.getItem('fullmall.supplier.secret') || '')
const connected = ref(false)
const loading = ref(false)
const error = ref('')
const success = ref('')
const orders = ref<SupplierOrder[]>([])
const statusFilter = ref('ALLOCATED')
const feedRows = reactive<FeedRow[]>([
  { supplierSkuCode: 'SUP-DROPSHIP-001', availableQuantity: 100, priceAmount: 35 }
])
const shippingOrderId = ref('')
const shippingForm = reactive({ carrierCode: 'SF', trackingNo: '' })

const statusLabels: Record<string, string> = {
  ALLOCATED: '待接单',
  ACCEPTED: '已接单',
  SHIPPED: '已发货',
  CANCELLED: '已取消',
  COMPLETED: '已完成'
}

onMounted(async () => {
  if (accessKey.value && secret.value && accessKey.value.startsWith('sup_')) {
    await connect()
  }
})

watch(statusFilter, () => {
  void loadOrders()
})

async function connect() {
  if (!accessKey.value.trim() || !secret.value.trim()) {
    error.value = '请输入 Access Key 和 Secret'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const ping = await signedRequest<{ supplierName: string }>('GET', '/open/v1/ping')
    sessionStorage.setItem('fullmall.supplier.key', accessKey.value.trim())
    sessionStorage.setItem('fullmall.supplier.secret', secret.value.trim())
    connected.value = true
    success.value = `已连接：${ping.supplierName}`
    await loadOrders()
  } catch (cause) {
    connected.value = false
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

async function loadOrders() {
  if (!connected.value) return
  loading.value = true
  error.value = ''
  try {
    const query = statusFilter.value ? `?status=${encodeURIComponent(statusFilter.value)}` : ''
    orders.value = await signedRequest<SupplierOrder[]>('GET', `/open/v1/orders${query}`)
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

async function accept(order: SupplierOrder) {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await signedRequest('POST', `/open/v1/orders/${order.fulfillmentId}/accept`, { note: '供应商工作台接单' })
    success.value = `订单 ${order.orderNo} 已接单`
    await loadOrders()
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

function openShip(order: SupplierOrder) {
  shippingOrderId.value = order.fulfillmentId
  shippingForm.carrierCode = 'SF'
  shippingForm.trackingNo = ''
}

async function ship(order: SupplierOrder) {
  if (!shippingForm.trackingNo.trim()) {
    error.value = '请填写运单号'
    return
  }
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await signedRequest('POST', `/open/v1/orders/${order.fulfillmentId}/ship`, {
      carrierCode: shippingForm.carrierCode.trim(),
      trackingNo: shippingForm.trackingNo.trim()
    })
    shippingOrderId.value = ''
    success.value = `订单 ${order.orderNo} 已发货`
    await loadOrders()
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

async function reject(order: SupplierOrder) {
  const reason = window.prompt('请输入拒单原因（缺货、超区、停产等）', '缺货')
  if (!reason) return
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    await signedRequest('POST', `/open/v1/orders/${order.fulfillmentId}/reject`, { reason })
    success.value = `订单 ${order.orderNo} 已拒单`
    await loadOrders()
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

function addFeedRow() {
  feedRows.push({ supplierSkuCode: '', availableQuantity: 0, priceAmount: 0 })
}

function removeFeedRow(index: number) {
  feedRows.splice(index, 1)
}

async function submitFeed() {
  loading.value = true
  error.value = ''
  success.value = ''
  try {
    const result = await signedRequest<{ updated: number }>('POST', '/open/v1/inventory/feed', {
      items: feedRows
        .filter(row => row.supplierSkuCode.trim())
        .map(row => ({
          supplierSkuCode: row.supplierSkuCode.trim(),
          availableQuantity: Math.max(0, Math.trunc(row.availableQuantity)),
          priceAmount: Math.max(0, Math.round(Number(row.priceAmount) * 100)),
          priceCurrency: 'CNY'
        }))
    })
    success.value = `库存/价格同步完成，共 ${result.updated} 条`
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

async function signedRequest<T>(method: string, path: string, body?: unknown): Promise<T> {
  const rawBody = body === undefined ? '' : JSON.stringify(body)
  const timestamp = String(Math.floor(Date.now() / 1000))
  const nonce = crypto.randomUUID().replace(/-/g, '')
  const bodyHash = await sha256Hex(rawBody)
  const canonical = `${method}\n${path}\n${timestamp}\n${nonce}\n${bodyHash}`
  const signature = await hmacHex(secret.value.trim(), canonical)
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: {
      'Content-Type': 'application/json',
      'X-Supplier-Key': accessKey.value.trim(),
      'X-Supplier-Timestamp': timestamp,
      'X-Supplier-Nonce': nonce,
      'X-Supplier-Signature': signature
    },
    body: body === undefined ? undefined : rawBody
  })
  if (!response.ok) {
    const text = await response.text()
    try {
      const parsed = JSON.parse(text) as { message?: string; code?: string }
      throw new Error(parsed.message || parsed.code || text)
    } catch (cause) {
      if (cause instanceof Error && !(cause instanceof SyntaxError)) throw cause
      throw new Error(text)
    }
  }
  return response.json() as Promise<T>
}

async function sha256Hex(text: string) {
  const digest = await crypto.subtle.digest('SHA-256', encode(text))
  return hex(digest)
}

function encode(text: string): ArrayBuffer {
  return new TextEncoder().encode(text).buffer as ArrayBuffer
}

async function hmacHex(key: string, message: string) {
  const cryptoKey = await crypto.subtle.importKey(
    'raw',
    encode(key),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const signature = await crypto.subtle.sign('HMAC', cryptoKey, encode(message))
  return hex(signature)
}

function hex(buffer: ArrayBuffer) {
  return Array.from(new Uint8Array(buffer)).map(byte => byte.toString(16).padStart(2, '0')).join('')
}

function money(value?: { amount: number; currency: string }) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}

function messageOf(cause: unknown) {
  return cause instanceof Error ? cause.message : '请求失败'
}

function disconnect() {
  sessionStorage.removeItem('fullmall.supplier.key')
  sessionStorage.removeItem('fullmall.supplier.secret')
  connected.value = false
  orders.value = []
  success.value = ''
  error.value = ''
}
</script>

<template>
  <div class="supplier-shell">
    <header class="supplier-header">
      <div>
        <span class="supplier-mark">FM</span>
        <div>
          <strong>供应商工作台</strong>
          <small>HMAC 签名 · 代发订单 · 库存与价格同步</small>
        </div>
      </div>
      <button v-if="connected" type="button" class="ghost-button" @click="disconnect">断开连接</button>
    </header>

    <main class="supplier-main">
      <section v-if="!connected" class="panel connect-panel">
        <p class="eyebrow">Supplier API v1</p>
        <h1>连接供应商开放接口</h1>
        <p class="muted">
          在运营后台「供应商」页面生成 Access Key 和 Secret。Secret 只显示一次，浏览器仅保存在当前会话。
        </p>
        <div v-if="error" class="alert error">{{ error }}</div>
        <label>
          <span>Access Key</span>
          <input v-model="accessKey" autocomplete="off" placeholder="sup_..." />
        </label>
        <label>
          <span>Secret</span>
          <input v-model="secret" type="password" autocomplete="off" placeholder="sec_..." />
        </label>
        <button type="button" class="primary-button" :disabled="loading" @click="connect">
          {{ loading ? '连接中…' : '连接并校验签名' }}
        </button>
      </section>

      <template v-else>
        <div v-if="error" class="alert error">{{ error }}</div>
        <div v-if="success" class="alert success">{{ success }}</div>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>代发订单</h2>
              <p class="muted">拉取 → 接单 → 发货/拒单。发货后平台扣减供应商库存并通知顾客。</p>
            </div>
            <div class="filters">
              <select v-model="statusFilter">
                <option value="">全部状态</option>
                <option value="ALLOCATED">待接单</option>
                <option value="ACCEPTED">已接单</option>
                <option value="SHIPPED">已发货</option>
                <option value="CANCELLED">已取消</option>
              </select>
              <button type="button" class="ghost-button" @click="loadOrders">刷新</button>
            </div>
          </div>

          <div v-if="!orders.length" class="empty">当前没有符合条件的订单</div>
          <article v-for="order in orders" :key="order.fulfillmentId" class="order-card">
            <div class="order-head">
              <div>
                <strong>{{ order.orderNo }}</strong>
                <span class="status" :class="order.status.toLowerCase()">{{ statusLabels[order.status] || order.status }}</span>
              </div>
              <strong>{{ money(order.total) }}</strong>
            </div>
            <div class="order-meta">
              <span>{{ order.contactName }} · {{ order.contactPhone }}</span>
              <span>{{ order.addressJson || '门店/地址见平台订单' }}</span>
            </div>
            <table class="line-table">
              <thead>
                <tr><th>商品</th><th>供应商 SKU</th><th>数量</th><th>单价</th></tr>
              </thead>
              <tbody>
                <tr v-for="line in order.lines" :key="line.skuId">
                  <td>{{ line.title }} <small>{{ line.skuCode }}</small></td>
                  <td>{{ line.supplierSkuCode || '未映射' }}</td>
                  <td>{{ line.quantity }}</td>
                  <td>{{ money(line.unitPrice) }}</td>
                </tr>
              </tbody>
            </table>
            <div v-if="order.trackingNo" class="tracking">
              运单：{{ order.carrierCode }} {{ order.trackingNo }}
            </div>
            <div v-if="order.note" class="tracking">备注：{{ order.note }}</div>
            <div class="order-actions">
              <button
                v-if="order.status === 'ALLOCATED'"
                type="button"
                class="primary-button small"
                :disabled="loading"
                @click="accept(order)"
              >
                接单
              </button>
              <button
                v-if="order.status === 'ACCEPTED'"
                type="button"
                class="primary-button small"
                :disabled="loading"
                @click="openShip(order)"
              >
                发货
              </button>
              <button
                v-if="order.status === 'ALLOCATED' || order.status === 'ACCEPTED'"
                type="button"
                class="danger-button small"
                :disabled="loading"
                @click="reject(order)"
              >
                拒单
              </button>
            </div>
            <div v-if="shippingOrderId === order.fulfillmentId" class="ship-form">
              <input v-model="shippingForm.carrierCode" placeholder="承运商，例如 SF" />
              <input v-model="shippingForm.trackingNo" placeholder="运单号" />
              <button type="button" class="primary-button small" :disabled="loading" @click="ship(order)">确认发货</button>
              <button type="button" class="ghost-button small" @click="shippingOrderId = ''">取消</button>
            </div>
          </article>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <h2>库存与价格同步</h2>
              <p class="muted">按供应商 SKU 编码提交可用库存和供货价；平台同步供应商虚拟仓库存。</p>
            </div>
            <button type="button" class="ghost-button" @click="addFeedRow">添加一行</button>
          </div>
          <table class="line-table">
            <thead>
              <tr><th>供应商 SKU 编码</th><th>可用库存</th><th>供货价（元）</th><th></th></tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in feedRows" :key="index">
                <td><input v-model="row.supplierSkuCode" placeholder="SUP-..." /></td>
                <td><input v-model.number="row.availableQuantity" type="number" min="0" /></td>
                <td><input v-model.number="row.priceAmount" type="number" min="0" step="0.01" /></td>
                <td><button type="button" class="ghost-button small" @click="removeFeedRow(index)">删除</button></td>
              </tr>
            </tbody>
          </table>
          <button type="button" class="primary-button" :disabled="loading" @click="submitFeed">提交库存/价格</button>
        </section>

        <section class="panel security-note">
          <h2>安全说明</h2>
          <ul>
            <li>每个请求使用 HMAC-SHA256 签名，覆盖 method、path+query、timestamp、nonce、body SHA-256。</li>
            <li>时间戳允许 ±5 分钟；nonce 重放返回 409。</li>
            <li>Secret 由运营后台生成并用服务端密钥加密存储；本页仅保存在当前浏览器会话。</li>
            <li>正式环境请配置 <code>SUPPLIER_CREDENTIAL_KEY</code>，并通过 HTTPS 访问。</li>
          </ul>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.supplier-shell {
  min-height: 100vh;
  background: #f4f3ef;
}

.supplier-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 28px;
  background: #1f2421;
  color: #fff;
}

.supplier-header > div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.supplier-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  background: #e1251b;
  font-weight: 800;
}

.supplier-header strong,
.supplier-header small {
  display: block;
}

.supplier-header small {
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}

.supplier-main {
  width: min(1080px, calc(100% - 32px));
  margin: 24px auto 48px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.panel {
  padding: 22px;
  border: 1px solid #dedbd3;
  border-radius: 16px;
  background: #fff;
}

.connect-panel {
  display: flex;
  max-width: 480px;
  margin: 40px auto;
  flex-direction: column;
  gap: 14px;
}

.connect-panel h1 {
  margin: 0;
}

.connect-panel label {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 13px;
  color: #5c574e;
}

input,
select {
  width: 100%;
  padding: 9px 10px;
  border: 1px solid #d8d4ca;
  border-radius: 8px;
  font: inherit;
  background: #fff;
}

.panel-head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.panel-head h2,
.security-note h2 {
  margin: 0;
}

.filters {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filters select {
  width: 140px;
}

.eyebrow {
  margin: 0;
  color: #b0451f;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.muted {
  color: #6b716c;
  font-size: 12px;
}

.alert {
  padding: 10px 12px;
  border-radius: 8px;
  font-size: 13px;
}

.alert.error {
  background: #fdecec;
  color: #a51f18;
}

.alert.success {
  background: #eef8f0;
  color: #22643d;
}

.primary-button,
.danger-button,
.ghost-button {
  border: 0;
  border-radius: 8px;
  padding: 10px 14px;
  font: inherit;
  cursor: pointer;
}

.primary-button {
  background: #e1251b;
  color: #fff;
  font-weight: 700;
}

.primary-button:disabled,
.danger-button:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.danger-button {
  background: #fdecec;
  color: #a51f18;
}

.ghost-button {
  background: #f2f0ea;
  color: #3b3f3b;
}

.small {
  padding: 7px 11px;
  font-size: 12px;
}

.empty {
  padding: 24px;
  text-align: center;
  color: #6b716c;
}

.order-card {
  padding: 16px;
  border: 1px solid #e3dfd6;
  border-radius: 12px;
  background: #fbfaf7;
}

.order-card + .order-card {
  margin-top: 12px;
}

.order-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status {
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f2f0ea;
  font-size: 11px;
}

.status.allocated {
  background: #fff3d8;
  color: #8a5a00;
}

.status.accepted {
  background: #e8f0fd;
  color: #24579b;
}

.status.shipped {
  background: #eef8f0;
  color: #22643d;
}

.status.cancelled {
  background: #fdecec;
  color: #a51f18;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin: 8px 0;
  color: #6b716c;
  font-size: 12px;
}

.line-table {
  width: 100%;
  border-collapse: collapse;
  margin: 8px 0;
  font-size: 13px;
}

.line-table th,
.line-table td {
  padding: 8px;
  border-bottom: 1px solid #e8e4db;
  text-align: left;
}

.line-table th {
  color: #6b716c;
  font-weight: 600;
}

.line-table small {
  color: #9a9e99;
}

.tracking {
  margin-top: 6px;
  color: #3b3f3b;
  font-size: 12px;
}

.order-actions,
.ship-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.ship-form {
  padding: 10px;
  border-radius: 8px;
  background: #f2f0ea;
}

.ship-form input {
  width: 180px;
}

.security-note ul {
  margin: 8px 0 0;
  padding-left: 20px;
  color: #5c574e;
  font-size: 13px;
  line-height: 1.8;
}
</style>