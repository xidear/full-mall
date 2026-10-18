<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'

interface ChatMessage {
  id: number
  direction: 'in' | 'out' | 'system'
  text: string
  at: string
}

const wsBase = import.meta.env.VITE_WS_BASE || 'ws://localhost:8081'
const messages = ref<ChatMessage[]>([])
const input = ref('')
const status = ref<'connecting' | 'open' | 'closed' | 'error'>('connecting')
const error = ref('')
let socket: WebSocket | null = null
let sequence = 0

onMounted(connect)
onBeforeUnmount(() => socket?.close())

function connect() {
  status.value = 'connecting'
  socket = new WebSocket(`${wsBase}/ws/support`)
  socket.onopen = () => {
    status.value = 'open'
    push('system', '已连接客服 WS。演示模式只回放，不接收真实消息。')
  }
  socket.onmessage = event => {
    let text = String(event.data)
    try {
      const payload = JSON.parse(text)
      if (payload.type === 'welcome') {
        text = `欢迎：协议 ${payload.protocol}，只读=${payload.readOnly}`
      } else if (payload.type === 'demo_read_only') {
        text = payload.message
      } else if (payload.type === 'echo') {
        text = payload.received
      }
    } catch {
      // Keep the raw payload when the server sends non-JSON data.
    }
    push('in', text)
  }
  socket.onerror = () => {
    status.value = 'error'
    error.value = 'WS 连接失败，请确认 ws 服务运行在 8081'
  }
  socket.onclose = () => {
    status.value = 'closed'
  }
}

function send() {
  const text = input.value.trim()
  if (!text || !socket || socket.readyState !== WebSocket.OPEN) {
    return
  }
  socket.send(JSON.stringify({ type: 'message', text }))
  push('out', text)
  input.value = ''
}

function push(direction: ChatMessage['direction'], text: string) {
  messages.value.push({
    id: ++sequence,
    direction,
    text,
    at: new Date().toLocaleTimeString('zh-CN')
  })
}
</script>

<template>
  <div class="support-shell">
    <header class="support-header">
      <div>
        <span class="support-mark">FM</span>
        <div>
          <strong>客服工作台</strong>
          <small>WS 会话 · 订单上下文 · AI 兜底预留</small>
        </div>
      </div>
      <span class="status" :class="status">{{ status }}</span>
    </header>

    <main class="support-main">
      <section class="conversation-panel">
        <div class="conversation-head">
          <div>
            <strong>演示会话</strong>
            <p>订单 / 物流 / 门店 / 批次上下文将在 Phase 5 接入</p>
          </div>
          <span class="agent-pill">AI + 人工</span>
        </div>

        <div class="messages">
          <div v-for="message in messages" :key="message.id" class="message" :class="message.direction">
            <div class="bubble">{{ message.text }}</div>
            <span class="time">{{ message.at }}</span>
          </div>
        </div>

        <div class="composer">
          <input v-model="input" type="text" placeholder="输入消息，回车发送" @keyup.enter="send">
          <button type="button" @click="send">发送</button>
        </div>
        <p v-if="error" class="error">{{ error }}</p>
      </section>

      <aside class="side-panel">
        <h2>会话上下文</h2>
        <dl>
          <dt>渠道</dt>
          <dd>Web / 小程序 / APP</dd>
          <dt>语言</dt>
          <dd>中文（后续多语言）</dd>
          <dt>Bot 策略</dt>
          <dd>规则 FAQ → RAG + 工具 → 人工</dd>
          <dt>安全</dt>
          <dd>PII 脱敏、限流、审计、人工兜底</dd>
        </dl>
        <div class="notice">
          AI 机器人接口已预留：知识库、订单/物流工具、引用来源、置信度与转人工。
        </div>
      </aside>
    </main>
  </div>
</template>