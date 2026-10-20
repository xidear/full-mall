<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface SetupItem { key: string; value: string; configured: boolean; hint: string }
interface SetupStatus { completed: boolean; ready: boolean; items: SetupItem[] }

const active = ref(0)
const loading = ref(false)
const saving = ref(false)
const status = ref<SetupStatus | null>(null)

// 只保存本向导负责的键；其余键由系统设置页维护，避免互相覆盖
const form = reactive<Record<string, string>>({
  site_name: '',
  default_locale: 'zh-CN',
  default_currency: 'CNY',
  'member.points.expiry-mode': 'NEVER',
  'member.points.expiry-days': '0',
  'distribution.level1-value': '5',
  'distribution.min-withdrawal': '10000',
  payment_provider: 'mock',
  logistics_provider: 'mock',
  sms_provider: 'mock',
  sms_access_key: '',
  sms_secret_key: '',
  logistics_key: '',
  logistics_customer: ''
})

const missing = computed(() => (status.value?.items ?? []).filter(item => !item.configured))

onMounted(load)

async function load() {
  loading.value = true
  try {
    const values = await api.get<Record<string, string>>('/admin/v1/settings')
    for (const key of Object.keys(form)) {
      const value = values[key]
      // 敏感键回显掩码，保持原样不动（回传掩码表示"不修改"）
      if (value !== undefined && value !== null) {
        form[key] = value
      }
    }
    status.value = await api.get<SetupStatus>('/admin/v1/setup/status')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '配置加载失败')
  } finally {
    loading.value = false
  }
}

async function saveStep() {
  saving.value = true
  try {
    await api.put('/admin/v1/settings', { ...form })
    status.value = await api.get<SetupStatus>('/admin/v1/setup/status')
    ElMessage.success('已保存')
    active.value = Math.min(active.value + 1, 3)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function finish() {
  saving.value = true
  try {
    await api.put('/admin/v1/settings', { ...form })
    status.value = await api.post<SetupStatus>('/admin/v1/setup/complete')
    ElMessage.success('首次配置已完成')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '完成失败')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>首次配置</h2>
        <p class="muted">可随时跳过，之后在「系统设置」里继续调整；未完成不影响使用。</p>
      </div>
      <div class="header-actions">
        <el-button @click="active = Math.min(active + 1, 3)">跳过本页</el-button>
        <el-button v-if="!status?.completed" type="primary" :loading="saving" @click="finish">直接完成</el-button>
        <el-tag v-else type="success">已完成</el-tag>
      </div>
    </div>

    <el-alert v-if="missing.length" type="warning" show-icon :closable="false" class="mb-16"
              :title="`还有 ${missing.length} 项待配置`" />

    <el-steps :active="active" finish-status="success" style="margin: 8px 0 20px">
      <el-step title="基础" description="站点与语言" />
      <el-step title="运营规则" description="积分与分销" />
      <el-step title="服务商" description="支付/物流/短信" />
      <el-step title="完成" />
    </el-steps>

    <el-form label-width="160px" v-if="active === 0">
      <el-form-item label="站点名称">
        <el-input v-model="form.site_name" placeholder="例如 Full Mall" />
      </el-form-item>
      <el-form-item label="默认语言">
        <el-select v-model="form.default_locale" style="width: 100%">
          <el-option label="简体中文" value="zh-CN" />
          <el-option label="English" value="en" />
        </el-select>
      </el-form-item>
      <el-form-item label="默认币种">
        <el-select v-model="form.default_currency" style="width: 100%">
          <el-option label="人民币 CNY" value="CNY" />
          <el-option label="港币 HKD" value="HKD" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="saveStep">保存并下一步</el-button>
    </el-form>

    <el-form label-width="160px" v-else-if="active === 1">
      <el-form-item label="积分过期规则">
        <el-select v-model="form['member.points.expiry-mode']" style="width: 100%">
          <el-option label="永不过期（默认）" value="NEVER" />
          <el-option label="按笔过期（FIFO）" value="FIFO" />
          <el-option label="统一有效期" value="ROLLING_DAYS" />
        </el-select>
      </el-form-item>
      <el-form-item label="过期天数">
        <el-input v-model="form['member.points.expiry-days']"
                  :disabled="form['member.points.expiry-mode'] === 'NEVER'"
                  placeholder="0 表示不过期" />
      </el-form-item>
      <el-form-item label="一级佣金（%）">
        <el-input v-model="form['distribution.level1-value']" />
      </el-form-item>
      <el-form-item label="最低提现（分）">
        <el-input v-model="form['distribution.min-withdrawal']" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="saveStep">保存并下一步</el-button>
    </el-form>

    <el-form label-width="160px" v-else-if="active === 2">
      <el-form-item label="支付服务商">
        <el-select v-model="form.payment_provider" style="width: 100%">
          <el-option label="mock（仅演示）" value="mock" />
          <el-option label="微信支付" value="wechat" />
          <el-option label="支付宝" value="alipay" />
        </el-select>
      </el-form-item>
      <el-form-item label="物流服务商">
        <el-select v-model="form.logistics_provider" style="width: 100%">
          <el-option label="mock（仅演示）" value="mock" />
          <el-option label="快递100" value="kuaidi100" />
        </el-select>
      </el-form-item>
      <el-form-item label="物流 Key">
        <el-input v-model="form.logistics_key" placeholder="留空表示不修改；已配置显示 ******" />
      </el-form-item>
      <el-form-item label="物流 Customer">
        <el-input v-model="form.logistics_customer" />
      </el-form-item>
      <el-form-item label="短信服务商">
        <el-select v-model="form.sms_provider" style="width: 100%">
          <el-option label="mock（仅演示）" value="mock" />
          <el-option label="阿里云" value="aliyun" />
          <el-option label="腾讯云" value="tencent" />
        </el-select>
      </el-form-item>
      <el-form-item label="短信 AccessKey">
        <el-input v-model="form.sms_access_key" placeholder="留空表示不修改" />
      </el-form-item>
      <el-form-item label="短信 Secret">
        <el-input v-model="form.sms_secret_key" type="password" placeholder="留空表示不修改" />
      </el-form-item>
      <el-button type="primary" :loading="saving" @click="saveStep">保存并下一步</el-button>
    </el-form>

    <div v-else>
      <el-result icon="success" title="配置已就绪">
        <template #sub-title>
          <p v-if="missing.length">仍有 {{ missing.length }} 项未配置：{{ missing.map(m => m.hint).join('；') }}</p>
          <p v-else>全部决策项已配置，可以开始运营。</p>
        </template>
        <template #extra>
          <el-button type="primary" :loading="saving" @click="finish">完成并进入后台</el-button>
          <el-button @click="active = 0">返回修改</el-button>
        </template>
      </el-result>
    </div>
  </div>
</template>
