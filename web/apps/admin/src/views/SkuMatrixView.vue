<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Result { created: number; skipped: number; codes: string[]; disabled?: number }

const productId = ref('')
const prefix = ref('')
const optionName = ref('规格')
const values = ref('')
const priceYuan = ref(0)
const loading = ref(false)
const result = ref<Result | null>(null)

// 多维度生成（颜色 × 存储 …）
const mode = ref<'single' | 'multi'>('single')
const dimensions = ref('颜色:黑,白\n存储:256G,512G')
const disabled = ref('')
const multiLoading = ref(false)
const multiResult = ref<Result | null>(null)

function parseDimensions(text: string): Record<string, string[]> {
  const result: Record<string, string[]> = {}
  for (const line of text.split('\n')) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const index = trimmed.indexOf(':')
    if (index <= 0) throw new Error(`格式应为 维度:值1,值2 —— ${trimmed}`)
    const name = trimmed.slice(0, index).trim()
    const values = trimmed.slice(index + 1).split(/[,，]/).map(item => item.trim()).filter(Boolean)
    if (!values.length) throw new Error(`维度「${name}」没有取值`)
    result[name] = values
  }
  if (!Object.keys(result).length) throw new Error('至少填写一个维度')
  return result
}

function parseDisabled(text: string): Array<Record<string, string>> {
  const rules: Array<Record<string, string>> = []
  for (const line of text.split('\n')) {
    const trimmed = line.trim()
    if (!trimmed) continue
    const rule: Record<string, string> = {}
    for (const pair of trimmed.split(/[,，]/)) {
      const index = pair.indexOf(':')
      if (index <= 0) throw new Error(`禁用组合格式应为 维度:值,... —— ${trimmed}`)
      rule[pair.slice(0, index).trim()] = pair.slice(index + 1).trim()
    }
    rules.push(rule)
  }
  return rules
}

async function generateMulti() {
  if (multiLoading.value) return
  if (!productId.value.trim() || !prefix.value.trim()) {
    ElMessage.error('请填写商品 ID 与 SKU 前缀')
    return
  }
  if (priceYuan.value <= 0) {
    ElMessage.error('价格必须大于 0')
    return
  }
  multiLoading.value = true
  multiResult.value = null
  try {
    multiResult.value = await api.post<Result>(
      `/admin/v1/products/${productId.value.trim()}/sku-matrix-multi`, {
        prefix: prefix.value.trim(),
        dimensions: parseDimensions(dimensions.value),
        disabled: parseDisabled(disabled.value),
        priceAmount: Math.round(priceYuan.value * 100)
      })
    ElMessage.success(`已生成 ${multiResult.value.created} 个 SKU`)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '生成失败')
  } finally {
    multiLoading.value = false
  }
}

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
        <ProductPicker v-model="productId" />
      </el-form-item>
      <el-form-item label="SKU 前缀">
        <el-input v-model="prefix" placeholder="例如 MUG" />
      </el-form-item>
      <el-form-item label="规格名">
        <el-input v-model="optionName" placeholder="例如 颜色" />
      </el-form-item>
      <el-form-item label="生成方式">
        <el-radio-group v-model="mode">
          <el-radio-button value="single">单维度</el-radio-button>
          <el-radio-button value="multi">多维度</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="mode === 'single'" label="规格值">
        <el-input v-model="values" type="textarea" :rows="3"
                  placeholder="用逗号或换行分隔，例如：米白,墨绿" />
      </el-form-item>
      <template v-else>
        <el-form-item label="维度">
          <el-input v-model="dimensions" type="textarea" :rows="4"
                    placeholder="每行 维度:值1,值2，例如：&#10;颜色:黑,白&#10;存储:256G,512G" />
        </el-form-item>
        <el-form-item label="禁用组合">
          <el-input v-model="disabled" type="textarea" :rows="2"
                    placeholder="每行一个组合，例如：颜色:白,存储:512G（可留空）" />
        </el-form-item>
      </template>
      <el-form-item label="统一价格（元）">
        <el-input-number v-model="priceYuan" :min="0.01" :step="1" :precision="2" />
      </el-form-item>
      <el-form-item>
        <el-button v-if="mode === 'single'" type="primary" :loading="loading" @click="generate">
          生成 SKU
        </el-button>
        <el-button v-else type="primary" :loading="multiLoading" @click="generateMulti">
          生成组合 SKU
        </el-button>
      </el-form-item>
    </el-form>

    <el-descriptions v-if="multiResult" :column="2" border>
      <el-descriptions-item label="已创建">{{ multiResult.created }}</el-descriptions-item>
      <el-descriptions-item label="已跳过">{{ multiResult.skipped }}</el-descriptions-item>
      <el-descriptions-item label="已禁用组合">{{ multiResult.disabled }}</el-descriptions-item>
      <el-descriptions-item label="编码" :span="2">
        <code v-for="code in multiResult.codes" :key="code" class="code-item">{{ code }}</code>
      </el-descriptions-item>
    </el-descriptions>

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
