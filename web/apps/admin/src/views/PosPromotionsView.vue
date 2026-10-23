<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Promotion {
  id: string
  code: string
  title: string
  type: string
  channel: string
  status: string
  startsAt: string
  endsAt: string
}

const rows = ref<Promotion[]>([])
const loading = ref(false)
const saving = ref(false)
const form = ref({
  code: '',
  title: '',
  type: 'FLASH_SALE',
  thresholdYuan: 0,
  valueYuan: 0,
  priority: 10,
  startsAt: '',
  endsAt: ''
})

const posRows = computed(() => rows.value.filter(row => (row.channel || '').toUpperCase() === 'POS'))

async function load() {
  loading.value = true
  try {
    rows.value = await api.get<Promotion[]>('/admin/v1/promotions')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载失败')
  } finally {
    loading.value = false
  }
}

async function create() {
  if (saving.value) return
  if (!form.value.code.trim() || !form.value.title.trim()) {
    ElMessage.error('编码与名称必填')
    return
  }
  if (!form.value.startsAt || !form.value.endsAt) {
    ElMessage.error('必须填写起止时间')
    return
  }
  if (new Date(form.value.endsAt) <= new Date(form.value.startsAt)) {
    ElMessage.error('结束时间必须晚于开始时间')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/promotions', {
      code: form.value.code.trim(),
      title: form.value.title.trim(),
      type: form.value.type,
      thresholdAmount: Math.round(form.value.thresholdYuan * 100),
      value: Math.round(form.value.valueYuan * 100),
      scope: 'ALL',
      scopeIds: [],
      channel: 'POS',
      startsAt: new Date(form.value.startsAt).toISOString(),
      endsAt: new Date(form.value.endsAt).toISOString(),
      priority: form.value.priority
    })
    ElMessage.success('已创建 POS 促销')
    form.value.code = ''
    form.value.title = ''
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '创建失败')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>POS 专属促销</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="这里的促销只在门店收银生效（channel=POS），线上不会命中"
              description="线上促销请在「促销管理」里建；两套互不影响。" />

    <el-form label-width="110px" style="max-width: 720px">
      <el-form-item label="编码"><el-input v-model="form.code" placeholder="POS-2026-01" /></el-form-item>
      <el-form-item label="名称"><el-input v-model="form.title" placeholder="门店满减" /></el-form-item>
      <el-form-item label="类型">
        <el-select v-model="form.type" style="width: 220px">
          <el-option label="秒杀" value="FLASH_SALE" />
          <el-option label="买赠" value="GIFT" />
          <el-option label="拼团" value="GROUP_BUY" />
        </el-select>
      </el-form-item>
      <el-form-item label="门槛（元）">
        <el-input-number v-model="form.thresholdYuan" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="优惠（元）">
        <el-input-number v-model="form.valueYuan" :min="0" :precision="2" />
      </el-form-item>
      <el-form-item label="优先级">
        <el-input-number v-model="form.priority" :min="0" :max="9999" />
      </el-form-item>
      <el-form-item label="开始时间">
        <el-date-picker v-model="form.startsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
      </el-form-item>
      <el-form-item label="结束时间">
        <el-date-picker v-model="form.endsAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="create">创建 POS 促销</el-button>
        <el-button :loading="loading" @click="load">刷新列表</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="posRows" v-loading="loading" empty-text="还没有 POS 专属促销">
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column prop="title" label="名称" min-width="180" />
      <el-table-column prop="type" label="类型" width="130" />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="startsAt" label="开始" min-width="180" />
      <el-table-column prop="endsAt" label="结束" min-width="180" />
    </el-table>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
</style>
