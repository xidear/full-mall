<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Result { created: number; skipped: number; codes: string[] }

const productId = ref('')
const prefix = ref('')
const optionName = ref('规格')
const values = ref('')
const priceYuan = ref(0)
const loading = ref(false)
const result = ref<Result | null>(null)

async function generate() {
  if (loading.value) return
  if (!productId.value.trim() || !prefix.value.trim()) {
    ElMessage.error('请填写商品 ID 与 SKU 前缀')
    return
  }
  const list = values.value.split(/[,，\n]/).map(item => item.trim()).filter(Boolean)
  if (!list.length) {
    ElMessage.error('至少填写一个规格值')
    return
  }
  if (priceYuan.value <= 0) {
    ElMessage.error('价格必须大于 0')
    return
  }
  loading.value = true
  result.value = null
  try {
    result.value = await api.post<Result>(
      `/admin/v1/products/${productId.value.trim()}/sku-matrix`, {
        prefix: prefix.value.trim(),
        optionName: optionName.value.trim(),
        values: list,
        priceAmount: Math.round(priceYuan.value * 100),
        currency: null
      })
    ElMessage.success(`已生成 ${result.value.created} 个 SKU`)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '生成失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div>
    <h1>SKU 规格矩阵</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="按规格值批量生成 SKU：编码规则为「前缀-规格值」"
              description="已存在的编码会被跳过（不会覆盖已有价格）；生成后仍需到商品页核对库存与图片。" />

    <el-form label-width="110px" style="max-width: 640px">
      <el-form-item label="商品 ID">
        <el-input v-model="productId" placeholder="UUID" />
      </el-form-item>
      <el-form-item label="SKU 前缀">
        <el-input v-model="prefix" placeholder="例如 MUG" />
      </el-form-item>
      <el-form-item label="规格名">
        <el-input v-model="optionName" placeholder="例如 颜色" />
      </el-form-item>
      <el-form-item label="规格值">
        <el-input v-model="values" type="textarea" :rows="3"
                  placeholder="用逗号或换行分隔，例如：米白,墨绿" />
      </el-form-item>
      <el-form-item label="统一价格（元）">
        <el-input-number v-model="priceYuan" :min="0.01" :step="1" :precision="2" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="generate">生成 SKU</el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="result" :column="2" border>
      <el-descriptions-item label="已创建">{{ result.created }}</el-descriptions-item>
      <el-descriptions-item label="已跳过（编码已存在）">{{ result.skipped }}</el-descriptions-item>
      <el-descriptions-item label="生成的编码" :span="2">
        <code v-for="code in result.codes" :key="code" class="code-item">{{ code }}</code>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<style scoped>
.mb-16 { margin-bottom: 16px; }
.code-item { margin-right: 12px; }
</style>
