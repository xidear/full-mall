<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

interface Change { skuId: string; skuCode: string; before: number; after: number }

const productId = ref('')
const dimKey = ref('存储')
const dimValue = ref('256G')
const mode = ref<'ADD' | 'SET'>('ADD')
const valueYuan = ref(0)
const preview = ref<Change[]>([])
const previewing = ref(false)
const applying = ref(false)

function yuan(fen: number) { return (fen / 100).toFixed(2) }

async function doPreview() {
  if (previewing.value) return
  if (!productId.value.trim() || !dimKey.value.trim() || !dimValue.value.trim()) {
    ElMessage.error('请填写商品 ID、规格名与规格值')
    return
  }
  previewing.value = true
  preview.value = []
  try {
    preview.value = await api.post<Change[]>(
      `/admin/v1/products/${productId.value.trim()}/price-rule/preview`, {
        match: { [dimKey.value.trim()]: dimValue.value.trim() },
        mode: mode.value,
        value: Math.round(valueYuan.value * 100)
      })
    if (!preview.value.length) {
      ElMessage.warning('没有匹配的 SKU（检查规格值是否与 options 完全一致）')
    }
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '预览失败')
  } finally {
    previewing.value = false
  }
}

async function doApply() {
  if (applying.value || !preview.value.length) return
  try {
    await ElMessageBox.confirm(
      `将修改 ${preview.value.length} 个 SKU 的价格，确认应用？`, '确认应用',
      { type: 'warning' })
  } catch {
    return
  }
  applying.value = true
  try {
    const result = await api.post<{ updated: number }>(
      `/admin/v1/products/${productId.value.trim()}/price-rule/apply`, {
        match: { [dimKey.value.trim()]: dimValue.value.trim() },
        mode: mode.value,
        value: Math.round(valueYuan.value * 100)
      })
    ElMessage.success(`已更新 ${result.updated} 个 SKU`)
    await doPreview()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '应用失败')
  } finally {
    applying.value = false
  }
}
</script>

<template>
  <div>
    <h1>规格改价</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="按规格条件筛选后改价，只影响命中的 SKU"
              description="必须先预览再应用：预览只计算不落库，应用才会写入。" />

    <el-form label-width="100px" style="max-width: 680px">
      <el-form-item label="商品">
        <ProductPicker v-model="productId" />
      </el-form-item>
      <el-form-item label="规格条件">
        <div class="row">
          <el-input v-model="dimKey" placeholder="规格名，如 存储" style="width: 160px" />
          <span class="eq">=</span>
          <el-input v-model="dimValue" placeholder="规格值，如 256G" style="width: 160px" />
        </div>
      </el-form-item>
      <el-form-item label="改价方式">
        <el-radio-group v-model="mode">
          <el-radio-button value="ADD">加减</el-radio-button>
          <el-radio-button value="SET">设为</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item :label="mode === 'ADD' ? '调整金额（元）' : '设定价格（元）'">
        <el-input-number v-model="valueYuan" :step="10" :precision="2" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="previewing" @click="doPreview">预览</el-button>
        <el-button type="danger" :disabled="!preview.length" :loading="applying"
                   @click="doApply">应用改价</el-button>
      </el-form-item>
    </el-form>

    <el-table v-if="preview.length" :data="preview" border>
      <el-table-column prop="skuCode" label="SKU" min-width="220" />
      <el-table-column label="改前" width="140">
        <template #default="{ row }">{{ yuan(row.before) }} 元</template>
      </el-table-column>
      <el-table-column label="改后" width="140">
        <template #default="{ row }"><strong>{{ yuan(row.after) }} 元</strong></template>
      </el-table-column>
      <el-table-column label="变化" width="140">
        <template #default="{ row }">{{ yuan(row.after - row.before) }} 元</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.row { display: flex; align-items: center; gap: 8px; }
.eq { color: #909399; }
</style>
