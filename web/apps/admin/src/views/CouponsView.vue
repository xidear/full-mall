<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface CouponTemplate {
  id: string
  code: string
  title: string
  type: 'AMOUNT' | 'PERCENT'
  value: number
  minOrderAmount: number
  totalQuantity: number
  issuedQuantity: number
  perMemberLimit: number
  validityDays: number
  status: string
}

const templates = ref<CouponTemplate[]>([])
// 正在启用的券模板 ID：避免重复点击
const actionId = ref('')
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const dialog = ref(false)

const form = reactive({
  code: '',
  title: '',
  type: 'AMOUNT' as 'AMOUNT' | 'PERCENT',
  valueYuan: 5,
  percent: 90,
  minOrderYuan: 0,
  totalQuantity: 100,
  perMemberLimit: 1,
  validityDays: 30,
  scope: 'ALL' as 'ALL' | 'SKU' | 'CATEGORY' | 'BRAND',
  scopeIds: [] as string[],
  window: [new Date(), new Date(Date.now() + 30 * 24 * 3600 * 1000)] as Date[]
})

interface ScopeOption { id: string; label: string; extra?: string }
const scopeOptions = ref<ScopeOption[]>([])

async function load() {
  loading.value = true
  try {
    templates.value = await api.get<CouponTemplate[]>('/admin/v1/coupons/templates')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载券模板失败'
  } finally {
    loading.value = false
  }
}

async function loadScopeOptions(scope: string) {
  scopeOptions.value = []
  if (scope === 'ALL') return
  try {
    scopeOptions.value = await api.get<ScopeOption[]>(
      `/admin/v1/promotions/scope-options?scope=${scope}&limit=100`)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载范围选项失败'
  }
}

function openCreate() {
  form.code = ''
  form.title = ''
  form.type = 'AMOUNT'
  form.valueYuan = 5
  form.percent = 90
  form.minOrderYuan = 0
  form.totalQuantity = 100
  form.perMemberLimit = 1
  form.validityDays = 30
  form.scope = 'ALL'
  form.scopeIds = []
  form.window = [new Date(), new Date(Date.now() + 30 * 24 * 3600 * 1000)]
  scopeOptions.value = []
  dialog.value = true
}

async function submit() {
  if (form.window.length !== 2) {
    ElMessage.error('请选择发放时间')
    return
  }
  if (form.scope !== 'ALL' && form.scopeIds.length === 0) {
    ElMessage.error('非全场券必须选择范围')
    return
  }
  saving.value = true
  try {
    await api.post('/admin/v1/coupons/templates', {
      code: form.code.trim().toUpperCase(),
      title: form.title.trim(),
      type: form.type,
      value: form.type === 'AMOUNT' ? Math.round(form.valueYuan * 100) : form.percent,
      minOrderAmount: Math.round(form.minOrderYuan * 100),
      totalQuantity: form.totalQuantity,
      perMemberLimit: form.perMemberLimit,
      validityDays: form.validityDays,
      startsAt: form.window[0].toISOString(),
      endsAt: form.window[1].toISOString()
    })
    ElMessage.success('券模板已创建（草稿）')
    dialog.value = false
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '创建券模板失败'
  } finally {
    saving.value = false
  }
}

async function activate(template: CouponTemplate) {
  if (actionId.value) return
  actionId.value = template.id
  try {
    await api.post(`/admin/v1/coupons/templates/${template.id}/activate`)
    ElMessage.success('券模板已启用')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '启用失败')
  } finally {
    actionId.value = ''
  }
}

function valueLabel(template: CouponTemplate) {
  return template.type === 'AMOUNT'
    ? `${(template.value / 100).toFixed(2)} 元`
    : `${template.value / 10} 折`
}

onMounted(load)
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>优惠券</h2>
        <p class="muted">建券 → 会员领取 → 下单抵扣；库存与每人限领由服务端校验。</p>
      </div>
      <div class="header-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建券模板</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="templates" stripe>
      <el-table-column prop="code" label="编码" width="160" />
      <el-table-column prop="title" label="名称" min-width="160" />
      <el-table-column label="面额" width="120">
        <template #default="{ row }">{{ valueLabel(row) }}</template>
      </el-table-column>
      <el-table-column label="门槛" width="120">
        <template #default="{ row }">
          {{ row.minOrderAmount > 0 ? (row.minOrderAmount / 100).toFixed(2) + ' 元' : '无门槛' }}
        </template>
      </el-table-column>
      <el-table-column label="已发/总量" width="130">
        <template #default="{ row }">{{ row.issuedQuantity }} / {{ row.totalQuantity }}</template>
      </el-table-column>
      <el-table-column label="每人限领" width="100" prop="perMemberLimit" />
      <el-table-column label="有效天数" width="100" prop="validityDays" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          {{ { DRAFT: '草稿', ACTIVE: '发放中', ENDED: '已结束' }[row.status as string] ?? row.status }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status === 'DRAFT'" size="small" type="primary"
                     :loading="actionId === row.id" @click="activate(row)">启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="新建券模板" width="520px">
      <el-form label-width="110px">
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="例如 NEW-YEAR-50" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.title" placeholder="例如 新年满减券" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type">
            <el-option label="满减券" value="AMOUNT" />
            <el-option label="折扣券" value="PERCENT" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.type === 'AMOUNT'" label="面额（元）">
          <el-input-number v-model="form.valueYuan" :min="0.01" :precision="2" />
        </el-form-item>
        <el-form-item v-else label="折扣（1-99）">
          <el-input-number v-model="form.percent" :min="1" :max="99" />
        </el-form-item>
        <el-form-item label="使用门槛（元）">
          <el-input-number v-model="form.minOrderYuan" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="发放总量">
          <el-input-number v-model="form.totalQuantity" :min="1" />
        </el-form-item>
        <el-form-item label="每人限领">
          <el-input-number v-model="form.perMemberLimit" :min="1" />
        </el-form-item>
        <el-form-item label="有效天数">
          <el-input-number v-model="form.validityDays" :min="1" />
        </el-form-item>
        <el-form-item label="适用范围">
          <el-select v-model="form.scope" @change="loadScopeOptions(form.scope)">
            <el-option label="全场" value="ALL" />
            <el-option label="指定商品" value="SKU" />
            <el-option label="指定类目" value="CATEGORY" />
            <el-option label="指定品牌" value="BRAND" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.scope !== 'ALL'" label="范围项">
          <el-select v-model="form.scopeIds" multiple filterable style="width: 100%">
            <el-option v-for="option in scopeOptions" :key="option.id"
                       :label="option.extra ? `${option.label} · ${option.extra}` : option.label"
                       :value="option.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="发放时间">
          <el-date-picker v-model="form.window" type="datetimerange" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>
