<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'

interface Agent {
  id: string
  displayName: string
  role: string
}

interface Conversation {
  id: string
  subject: string
  orderId?: string
  status: string
  locale: string
  agentId?: string
  agentName?: string
  customerName?: string
  customerPhone?: string
  createdAt: string
  assignedAt?: string
  lastMessageAt?: string
}

interface Message {
  id: string
  senderType: string
  senderId?: string
  messageType: string
  content: string
  metadataJson?: string
  createdAt: string
}

interface ProductPage {
  items: Array<{
    id: string
    title: string
    skus: Array<{ id: string; skuCode: string; price: { amount: number; currency: string } }>
  }>
}

const API = import.meta.env.VITE_API_BASE || ''
const STOREFRONT = import.meta.env.VITE_STOREFRONT_BASE || 'http://localhost:3000'
const agents = ref<Agent[]>([])
const agentId = ref(localStorage.getItem('fullmall.support.agent') || '')
const conversations = ref<Conversation[]>([])
const selected = ref<Conversation | null>(null)
const messages = ref<Message[]>([])
const input = ref('')
const loading = ref(false)
const error = ref('')
const success = ref('')
const products = ref<ProductPage | null>(null)
const productDialog = ref(false)
const productForm = reactive({ productId: '', mode: 'PERMANENT' })
const helpDialog = ref(false)
const helpForm = reactive({ title: '', url: '' })
const transferDialog = ref(false)
const transferForm = reactive({ toAgentId: '', reason: '' })
let pollTimer: ReturnType<typeof setInterval> | null = null

const unassigned = computed(() => selected.value && !selected.value.agentId)
const canOperate = computed(() => selected.value && selected.value.agentId === agentId.value)

onMounted(async () => {
  await loadAgents()
  await loadConversations()
  pollTimer = setInterval(async () => {
    await loadConversations()
    if (selected.value) {
      await loadMessages(selected.value.id)
    }
  }, 10_000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(options?.headers ?? {}) }
  })
  if (!response.ok) {
    const text = await response.text()
    try {
      const parsed = JSON.parse(text) as { message?: string }
      throw new Error(parsed.message || text)
    } catch (cause) {
      if (cause instanceof Error && !(cause instanceof SyntaxError)) throw cause
      throw new Error(text)
    }
  }
  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}

async function loadAgents() {
  try {
    agents.value = await request<Agent[]>('/support/v1/agents')
    if (!agentId.value || !agents.value.some(agent => agent.id === agentId.value)) {
      agentId.value = agents.value[0]?.id ?? ''
    }
    if (agentId.value) {
      localStorage.setItem('fullmall.support.agent', agentId.value)
    }
    await loadProducts()
  } catch (cause) {
    error.value = messageOf(cause)
  }
}

async function onAgentChange() {
  localStorage.setItem('fullmall.support.agent', agentId.value)
  selected.value = null
  messages.value = []
  await loadConversations()
}

async function loadConversations() {
  if (!agentId.value) return
  try {
    conversations.value = await request<Conversation[]>(
      `/support/v1/conversations?agentId=${encodeURIComponent(agentId.value)}`
    )
    if (!selected.value && conversations.value.length) {
      await selectConversation(conversations.value[0])
    }
  } catch (cause) {
    error.value = messageOf(cause)
  }
}

async function loadProducts() {
  try {
    products.value = await request<ProductPage>('/api/v1/products?locale=zh-CN&size=50')
    productForm.productId = products.value.items[0]?.id ?? ''
  } catch {
    products.value = null
  }
}

async function selectConversation(conversation: Conversation) {
  selected.value = conversation
  await loadMessages(conversation.id)
}

async function loadMessages(conversationId: string) {
  try {
    messages.value = await request<Message[]>(
      `/support/v1/conversations/${conversationId}/messages?agentId=${encodeURIComponent(agentId.value)}`
    )
  } catch (cause) {
    error.value = messageOf(cause)
  }
}

async function claim() {
  if (!selected.value || !agentId.value) return
  loading.value = true
  try {
    selected.value = await request<Conversation>(
      `/support/v1/conversations/${selected.value.id}/claim`,
      { method: 'POST', body: JSON.stringify({ agentId: agentId.value }) }
    )
    success.value = '会话已领取'
    await loadConversations()
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

async function sendText() {
  if (!selected.value || !input.value.trim()) return
  loading.value = true
  try {
    await request(`/support/v1/conversations/${selected.value.id}/messages`, {
      method: 'POST',
      body: JSON.stringify({ agentId: agentId.value, content: input.value.trim() })
    })
    input.value = ''
    await loadMessages(selected.value.id)
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

function openProductDialog() {
  productForm.productId = products.value?.items[0]?.id ?? ''
  productForm.mode = 'PERMANENT'
  productDialog.value = true
}

async function sendProduct() {
  if (!selected.value || !productForm.productId) return
  loading.value = true
  try {
    await request(`/support/v1/conversations/${selected.value.id}/product`, {
      method: 'POST',
      body: JSON.stringify({
        agentId: agentId.value,
        productId: productForm.productId,
        mode: productForm.mode
      })
    })
    productDialog.value = false
    await loadMessages(selected.value.id)
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

async function sendHelp() {
  if (!selected.value || !helpForm.title.trim() || !helpForm.url.trim()) return
  loading.value = true
  try {
    await request(`/support/v1/conversations/${selected.value.id}/help-link`, {
      method: 'POST',
      body: JSON.stringify({
        agentId: agentId.value,
        title: helpForm.title.trim(),
        url: helpForm.url.trim()
      })
    })
    helpDialog.value = false
    helpForm.title = ''
    helpForm.url = ''
    await loadMessages(selected.value.id)
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

function openTransfer() {
  transferForm.toAgentId = agents.value.find(agent => agent.id !== agentId.value)?.id ?? ''
  transferForm.reason = ''
  transferDialog.value = true
}

async function transfer() {
  if (!selected.value || !transferForm.toAgentId) return
  loading.value = true
  try {
    await request(`/support/v1/conversations/${selected.value.id}/transfer`, {
      method: 'POST',
      body: JSON.stringify({
        fromAgentId: agentId.value,
        toAgentId: transferForm.toAgentId,
        reason: transferForm.reason.trim() || undefined
      })
    })
    transferDialog.value = false
    success.value = '会话已转交'
    await loadConversations()
  } catch (cause) {
    error.value = messageOf(cause)
  } finally {
    loading.value = false
  }
}

function metadata(message: Message) {
  if (!message.metadataJson) return null
  try {
    return JSON.parse(message.metadataJson) as Record<string, unknown>
  } catch {
    return null
  }
}

function productLink(message: Message) {
  const meta = metadata(message)
  return meta ? `${STOREFRONT}${meta.link}` : STOREFRONT
}

function money(meta: Record<string, unknown>) {
  const amount = Number(meta.priceAmount ?? 0)
  const currency = String(meta.currency ?? 'CNY')
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency }).format(amount / 100)
}

function messageOf(cause: unknown) {
  return cause instanceof Error ? cause.message : '请求失败'
}
</script>

<template>
  <div class="support-shell">
    <header class="support-header">
      <div>
        <span class="support-mark">FM</span>
        <div>
          <strong>客服工作台</strong>
          <small>会话分配 · 转单 · 商品推荐 · 帮助文档 · 推广佣金</small>
        </div>
      </div>
      <div class="agent-switch">
        <select v-model="agentId" @change="onAgentChange">
          <option v-for="agent in agents" :key="agent.id" :value="agent.id">
            {{ agent.displayName }}（{{ agent.role }}）
          </option>
        </select>
      </div>
    </header>

    <main class="support-grid">
      <aside class="conversation-list panel">
        <div class="panel-head">
          <h2>会话</h2>
          <button type="button" class="ghost" @click="loadConversations">刷新</button>
        </div>
        <div v-if="!conversations.length" class="empty">暂无会话</div>
        <button
          v-for="conversation in conversations"
          :key="conversation.id"
          type="button"
          class="conversation-item"
          :class="{ active: selected?.id === conversation.id }"
          @click="selectConversation(conversation)"
        >
          <div>
            <strong>{{ conversation.subject }}</strong>
            <span class="status" :class="conversation.agentId ? 'assigned' : 'waiting'">
              {{ conversation.agentId ? conversation.agentName || '已分配' : '待领取' }}
            </span>
          </div>
          <small>{{ conversation.customerName || '顾客' }} · {{ conversation.customerPhone || '未留手机号' }}</small>
          <small>{{ new Date(conversation.lastMessageAt || conversation.createdAt).toLocaleString('zh-CN') }}</small>
        </button>
      </aside>

      <section class="chat-panel panel">
        <template v-if="selected">
          <div class="chat-head">
            <div>
              <h2>{{ selected.subject }}</h2>
              <p class="muted">
                {{ selected.customerName || '顾客' }} · {{ selected.customerPhone || '未留手机号' }}
                <span v-if="selected.orderId"> · 订单 {{ selected.orderId.slice(0, 8) }}</span>
              </p>
            </div>
            <div class="chat-actions">
              <button v-if="unassigned" type="button" class="primary" :disabled="loading" @click="claim">领取会话</button>
              <button v-if="canOperate" type="button" class="ghost" @click="openTransfer">转单</button>
            </div>
          </div>

          <div v-if="error" class="alert error">{{ error }}</div>
          <div v-if="success" class="alert success">{{ success }}</div>

          <div class="messages">
            <div v-for="message in messages" :key="message.id" class="message" :class="message.senderType.toLowerCase()">
              <div v-if="message.messageType === 'PRODUCT'" class="product-card">
                <strong>{{ metadata(message)?.title || message.content }}</strong>
                <span>{{ metadata(message)?.skuCode }}</span>
                <strong>{{ money(metadata(message) || {}) }}</strong>
                <a :href="productLink(message)" target="_blank" rel="noopener">打开商品并锁定推广关系</a>
              </div>
              <div v-else-if="message.messageType === 'HELP_LINK'" class="help-card">
                <strong>{{ metadata(message)?.title || message.content }}</strong>
                <a :href="String(metadata(message)?.url || '#')" target="_blank" rel="noopener">打开帮助文档</a>
              </div>
              <div v-else class="bubble">{{ message.content }}</div>
              <span class="time">{{ new Date(message.createdAt).toLocaleTimeString('zh-CN') }}</span>
            </div>
            <div v-if="!messages.length" class="empty">暂无消息</div>
          </div>

          <div class="composer">
            <input v-model="input" type="text" placeholder="输入消息，回车发送" @keyup.enter="sendText">
            <button type="button" class="primary" :disabled="!canOperate || loading" @click="sendText">发送</button>
            <button type="button" class="ghost" :disabled="!canOperate" @click="openProductDialog">商品</button>
            <button type="button" class="ghost" :disabled="!canOperate" @click="helpDialog = true">帮助</button>
          </div>
          <p v-if="unassigned" class="muted">领取会话后才能回复、发送商品或转单。</p>
        </template>
        <div v-else class="empty">选择左侧会话开始处理</div>
      </section>
    </main>

    <div v-if="productDialog" class="modal-mask" @click.self="productDialog = false">
      <div class="modal">
        <h3>发送商品卡片</h3>
        <label>商品</label>
        <select v-model="productForm.productId">
          <option v-for="product in products?.items ?? []" :key="product.id" :value="product.id">
            {{ product.title }} · {{ product.skus[0]?.skuCode }}
          </option>
        </select>
        <label>推广绑定模式</label>
        <select v-model="productForm.mode">
          <option value="PERMANENT">永久绑定</option>
          <option value="REGISTER">注册绑定</option>
          <option value="ORDER">下单绑定</option>
          <option value="VISIT">访问绑定（30 天）</option>
          <option value="ONE_TIME">一次性绑定</option>
        </select>
        <p class="muted">顾客通过卡片下单并支付后，佣金记入当前客服；绑定策略与分销模块共用。</p>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="productDialog = false">取消</button>
          <button type="button" class="primary" :disabled="loading" @click="sendProduct">发送</button>
        </div>
      </div>
    </div>

    <div v-if="helpDialog" class="modal-mask" @click.self="helpDialog = false">
      <div class="modal">
        <h3>发送帮助文档</h3>
        <label>标题</label>
        <input v-model="helpForm.title" placeholder="例如：退换货政策" />
        <label>链接</label>
        <input v-model="helpForm.url" placeholder="https://..." />
        <p class="muted">链接在移动端会以卡片形式展示，可直接打开。</p>
        <div class="modal-actions">
          <button type="button" class="ghost" @click="helpDialog = false">取消</button>
          <button type="button" class="primary" :disabled="loading" @click="sendHelp">发送</button>
        </div>
      </div>
    </div>

    <div v-if="transferDialog" class="modal-mask" @click.self="transferDialog = false">
      <div class="modal">
        <h3>转交会话</h3>
        <label>转交给</label>
        <select v-model="transferForm.toAgentId">
          <option v-for="agent in agents.filter(item => item.id !== agentId)" :key="agent.id" :value="agent.id">
            {{ agent.displayName }}（{{ agent.role }}）
          </option>
        </select>
        <label>原因</label>
        <input v-model="transferForm.reason" placeholder="可选" />
        <div class="modal-actions">
          <button type="button" class="ghost" @click="transferDialog = false">取消</button>
          <button type="button" class="primary" :disabled="loading" @click="transfer">确认转单</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.support-shell {
  min-height: 100vh;
  background: #f4f3ef;
}

.support-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 24px;
  background: #1f2421;
  color: #fff;
}

.support-header > div {
  display: flex;
  align-items: center;
  gap: 12px;
}

.support-mark {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 12px;
  background: #e1251b;
  font-weight: 800;
}

.support-header small {
  display: block;
  color: rgba(255, 255, 255, 0.6);
  font-size: 12px;
}

.agent-switch select {
  min-width: 180px;
}

.support-grid {
  display: grid;
  grid-template-columns: minmax(260px, 0.8fr) minmax(0, 1.7fr);
  gap: 16px;
  width: min(1280px, calc(100% - 32px));
  margin: 20px auto 40px;
}

.panel {
  padding: 18px;
  border: 1px solid #dedbd3;
  border-radius: 14px;
  background: #fff;
}

.panel-head,
.chat-head,
.chat-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.panel-head h2,
.chat-head h2 {
  margin: 0;
  font-size: 17px;
}

.conversation-item {
  display: flex;
  width: 100%;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  margin-top: 8px;
  border: 1px solid #e3dfd6;
  border-radius: 10px;
  background: #fbfaf7;
  text-align: left;
  cursor: pointer;
}

.conversation-item.active {
  border-color: #e1251b;
  background: #fdf1ef;
}

.conversation-item > div {
  display: flex;
  justify-content: space-between;
  gap: 8px;
}

.status {
  padding: 1px 7px;
  border-radius: 999px;
  font-size: 11px;
}

.status.assigned {
  background: #eef8f0;
  color: #22643d;
}

.status.waiting {
  background: #fff3d8;
  color: #8a5a00;
}

.chat-panel {
  display: flex;
  min-height: 640px;
  flex-direction: column;
}

.messages {
  flex: 1;
  overflow: auto;
  padding: 14px 4px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.message {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 3px;
}

.message.agent {
  align-items: flex-end;
}

.bubble {
  max-width: min(520px, 86%);
  padding: 10px 12px;
  border-radius: 12px;
  background: #f2f0ea;
  white-space: pre-wrap;
  line-height: 1.55;
}

.message.agent .bubble {
  background: #e1251b;
  color: #fff;
}

.product-card,
.help-card {
  display: flex;
  max-width: 340px;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border: 1px solid #e3dfd6;
  border-radius: 12px;
  background: #fffdf8;
}

.product-card a,
.help-card a {
  color: #b0451f;
  font-weight: 700;
}

.time {
  color: #9a9e99;
  font-size: 11px;
}

.composer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e3dfd6;
}

.composer input {
  flex: 1;
  min-width: 180px;
}

button,
input,
select {
  padding: 9px 10px;
  border: 1px solid #d8d4ca;
  border-radius: 8px;
  font: inherit;
  background: #fff;
}

.primary {
  border-color: #e1251b;
  background: #e1251b;
  color: #fff;
  font-weight: 700;
  cursor: pointer;
}

.ghost {
  background: #f2f0ea;
  color: #3b3f3b;
  cursor: pointer;
}

.primary:disabled,
.ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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

.empty {
  padding: 24px;
  color: #6b716c;
  text-align: center;
}

.muted {
  color: #6b716c;
  font-size: 12px;
}

.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 20;
  display: grid;
  place-items: center;
  padding: 18px;
  background: rgba(20, 24, 21, 0.45);
}

.modal {
  display: flex;
  width: min(480px, 100%);
  flex-direction: column;
  gap: 10px;
  padding: 22px;
  border-radius: 16px;
  background: #fff;
}

.modal h3 {
  margin: 0;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

@media (max-width: 900px) {
  .support-grid {
    grid-template-columns: 1fr;
  }

  .chat-panel {
    min-height: auto;
  }

  .support-header {
    flex-wrap: wrap;
  }
}
</style>