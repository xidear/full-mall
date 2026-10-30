<script setup lang="ts">
/**
 * 税率规则（批次 E）。
 *
 * 税率以**基点**录入（10000 = 100%）：整数存储避免浮点税率与金额相乘产生 ±1 分错账。
 * 含税/净价是**该规则的口径**：含税 = 从价内反算，净价 = 另加到总额。
 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'
/** 辖区以国家码为主，但允许自定义（如 CN-VAT）：国家码不该靠手背。 */
const countryOptions = ref<Array<{ code: string; name: string }>>([])

interface RuleRow {
  id: string
  jurisdiction: string
  name: string
  rateBps: number
  priceIncludesTax: boolean
  active: boolean
}

const rows = ref<RuleRow[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const dialogVisible = ref(false)
const editing = reactive({
  id: '',
  jurisdiction: '',
  name: '',
  ratePercent: 0,
  priceIncludesTax: true,
  active: true
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    rows.value = await api.get<RuleRow[]>('/admin/v1/tax-rules')
  } catch (cause) {
    error.value = '税率规则加载失败：' + (cause as Error).message
  } finally {
    loading.value = false
  }
}

function openCreate() {
  Object.assign(editing, {
    id: '', jurisdiction: '', name: '', ratePercent: 0, priceIncludesTax: true, active: true
  })
  dialogVisible.value = true
}

function openEdit(row: RuleRow) {
  Object.assign(editing, {
    id: row.id,
    jurisdiction: row.jurisdiction,
    name: row.name,
    ratePercent: row.rateBps / 100,
    priceIncludesTax: row.priceIncludesTax,
    active: row.active
  })
  dialogVisible.value = true
}

async function save() {
  const rateBps = Math.round(editing.ratePercent * 100)
  if (rateBps < 0 || rateBps > 10000) {
    ElMessage.error('税率必须在 0%–100% 之间')
    return
  }
  saving.value = true
  try {
    const body = {
      jurisdiction: editing.jurisdiction.trim(),
      name: editing.name.trim(),
      rateBps,
      priceIncludesTax: editing.priceIncludesTax,
      active: editing.active
    }
    const path = editing.id ? `/admin/v1/tax-rules/${editing.id}` : '/admin/v1/tax-rules'
    rows.value = await api.put<RuleRow[]>(path, body)
    ElMessage.success('已保存')
    dialogVisible.value = false
  } catch (cause) {
    ElMessage.error('保存失败：' + (cause as Error).message)
  } finally {
    saving.value = false
  }
}

async function remove(row: RuleRow) {
  try {
    await api.delete(`/admin/v1/tax-rules/${row.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    ElMessage.error('删除失败：' + (cause as Error).message)
  }
}

async function loadCountries() {
  try {
    countryOptions.value = await api.get<Array<{ code: string; name: string }>>('/api/v1/regions/countries', { locale: 'zh-CN' })
  } catch (cause) {
    ElMessage.error('国家候选加载失败：' + (cause instanceof Error ? cause.message : ''))
  }
}

onMounted(async () => {
  await loadCountries()
  await load()
})
</script>

<template>
  <section class="tax-page">
    <div class="page-heading">
      <h1>税率规则</h1>
      <div class="actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新增税率</el-button>
      </div>
    </div>

    <p class="hint">
      辖区可以是国家码（目的地税，如 SG / US-CA）或市场的税务辖区（如 CN-VAT）。
      计税时<b>目的地优先、市场辖区兜底</b>；两者都没有时不会静默按 0 计税，会在日志与报关单里体现。
    </p>

    <el-alert v-if="error" :title="error" type="error" :closable="false" />

    <el-table :data="rows" v-loading="loading" empty-text="暂无税率规则">
      <el-table-column prop="jurisdiction" label="辖区" width="160" />
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column label="税率" width="100">
        <template #default="{ row }">{{ (row.rateBps / 100).toFixed(2) }}%</template>
      </el-table-column>
      <el-table-column label="口径" width="110">
        <template #default="{ row }">{{ row.priceIncludesTax ? '价内含税' : '净价另加' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : 'info'">{{ row.active ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editing.id ? '编辑税率' : '新增税率'" width="520px">
      <el-form label-width="110px">
        <el-form-item label="税务辖区">
          <el-select v-model="editing.jurisdiction" filterable allow-create default-first-option
                     placeholder="选择国家码，或直接输入自定义辖区（如 CN-VAT）" style="width: 100%">
            <el-option v-for="item in countryOptions" :key="item.code"
                       :label="item.name + '（' + item.code + '）'" :value="item.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="editing.name" placeholder="如 增值税 13%" />
        </el-form-item>
        <el-form-item label="税率（%）">
          <el-input-number v-model="editing.ratePercent" :min="0" :max="100" :precision="2" />
        </el-form-item>
        <el-form-item label="计税口径">
          <el-radio-group v-model="editing.priceIncludesTax">
            <el-radio :value="true">价内含税</el-radio>
            <el-radio :value="false">净价另加</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="启用">
          <el-switch v-model="editing.active" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-heading { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; }
.actions { display: flex; gap: 8px; }
.hint { color: var(--text-muted, #666); margin: 0 0 16px; line-height: 1.6; }
</style>
