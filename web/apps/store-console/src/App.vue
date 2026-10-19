<script setup lang="ts">
import { ref, watch } from 'vue'
import StoreConsoleView from './views/StoreConsoleView.vue'
import PosView from './views/PosView.vue'
import StaffLogin from './views/StaffLogin.vue'
import { auth, loginRequired, logout } from './auth'

const activeTab = ref<'operations' | 'pos'>('operations')
const posMounted = ref(false)
const needsLogin = ref(loginRequired() && !auth.token)

watch(activeTab, tab => {
  if (tab === 'pos') {
    posMounted.value = true
  }
})
</script>

<template>
  <StaffLogin v-if="needsLogin" @authenticated="needsLogin = false" />

  <div v-else class="store-shell">
    <header class="store-header">
      <div>
        <span class="store-mark">FM</span>
        <div>
          <strong>门店工作台</strong>
          <small>收货 · 批次 · 拣货 · 自提核销 · 收银</small>
        </div>
      </div>
      <div class="store-header-tools">
        <nav class="store-tabs">
          <button type="button" :class="{ active: activeTab === 'operations' }" @click="activeTab = 'operations'">
            门店作业
          </button>
          <button type="button" :class="{ active: activeTab === 'pos' }" @click="activeTab = 'pos'">
            收银台
          </button>
        </nav>
        <span class="store-mode">扫码优先 · 平板适配</span>
        <button v-if="loginRequired()" type="button" class="staff-logout" @click="logout(); needsLogin = true">
          退出登录
        </button>
      </div>
    </header>
    <main class="store-main">
      <div v-show="activeTab === 'operations'">
        <StoreConsoleView />
      </div>
      <div v-if="posMounted" v-show="activeTab === 'pos'">
        <PosView />
      </div>
    </main>
  </div>
</template>

<style scoped>
.store-header-tools {
  display: flex;
  align-items: center;
  gap: 18px;
}

.store-tabs {
  display: flex;
  gap: 6px;
  padding: 4px;
  background: rgba(255, 255, 255, 0.12);
  border-radius: 10px;
}

.store-tabs button {
  border: 0;
  border-radius: 8px;
  padding: 8px 16px;
  color: inherit;
  background: transparent;
  font: inherit;
  cursor: pointer;
}

.store-tabs button.active {
  background: #fff;
  color: #1f2b22;
  font-weight: 700;
}

.staff-logout {
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 8px;
  padding: 7px 12px;
  background: transparent;
  color: #fff;
  font: inherit;
  font-size: 12px;
  cursor: pointer;
}
</style>