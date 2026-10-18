<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ProductUpsertRequest } from '@fullmall/api-client'
import { api } from '../api'

const router = useRouter()
const saving = ref(false)
const error = ref('')

const categories = [
  { id: '99999999-9999-9999-9999-999999999999', name: '生鲜食品' },
  { id: '99999999-9999-9999-9999-999999999998', name: '家居日用' }
]

const optionNames = ['规格', '颜色', '容量', '口味', '尺寸']
const currencies = ['CNY', 'HKD', 'MOP', 'TWD']

interface VariantForm {
  skuCode: string
  barcode: string
  priceYuan: number
  currency: string
  optionName: string
  optionValue: string
}

interface PolicyForm {
  batchTracking: boolean
  pickingStrategy: 'FEFO' | 'FIFO' | 'NONE'
  minRemainingShelfLifeDays: number
  nearExpiryThresholdDays: number
  nearExpiryAction: 'NORMAL' | 'DISCOUNT' | 'DELIST' | 'WRITE_OFF'
  autoBlockExpired: boolean
}

const form = reactive({
  title: '',
  description: '',
  categoryId: categories[0].id,
  status: 'DRAFT' as 'DRAFT' | 'ACTIVE',
  inventoryTrackingLevel: 'VARIANT' as 'VARIANT' | 'PRODUCT',
  shelfLifeManaged: false,
  policy: {
    batchTracking: true,
    pickingStrategy: 'FEFO',
    minRemainingShelfLifeDays: 2,
    nearExpiryThresholdDays: 7,
    nearExpiryAction: 'DISCOUNT',
    autoBlockExpired: true
  } as PolicyForm,
  variants: [
    {
      skuCode: '',
      barcode: '',
      priceYuan: 0,
      currency: 'CNY',
      optionName: '规格',
      optionValue: ''
    }
  ] as VariantForm[]
})

function addVariant() {
  form.variants.push({
    skuCode: '',
    barcode: '',
    priceYuan: 0,
    currency: 'CNY',
    optionName: '规格',
    optionValue: ''
  })
}

function removeVariant(index: number) {
  if (form.variants.length === 1) {
    ElMessage.warning('至少保留一个规格')
    return
  }
  form.variants.splice(index, 1)
}

async function submit() {
  error.value = ''
  if (!form.title.trim()) {
    error.value = '请填写商品标题'
    return
  }
  if (form.variants.some(variant => !variant.skuCode.trim() || !variant.optionValue.trim())) {
    error.value = '请填写每个规格的 SKU 编码和规格值'
    return
  }
  if (form.variants.some(variant => variant.priceYuan <= 0)) {
    error.value = '价格必须大于 0'
    return
  }

  const payload: ProductUpsertRequest = {
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    categoryId: form.categoryId,
    status: form.status,
    inventoryTrackingLevel: form.inventoryTrackingLevel,
    shelfLifeManaged: form.shelfLifeManaged,
    skus: form.variants.map(variant => ({
      skuCode: variant.skuCode.trim(),
      barcode: variant.barcode.trim() || undefined,
      priceAmount: Math.round(variant.priceYuan * 100),
      currency: variant.currency,
      options: { [variant.optionName]: variant.optionValue.trim() },
      shelfLifePolicy: form.shelfLifeManaged
        ? {
            batchTracking: form.policy.batchTracking,
            pickingStrategy: form.policy.pickingStrategy,
            minRemainingShelfLifeDays: form.policy.minRemainingShelfLifeDays,
            nearExpiryThresholdDays: form.policy.nearExpiryThresholdDays,
            nearExpiryAction: form.policy.nearExpiryAction,
            autoBlockExpired: form.policy.autoBlockExpired
          }
        : undefined
    }))
  }

  saving.value = true
  try {
    await api.post('/admin/v1/products', payload)
    ElMessage.success('商品已创建')
    await router.push('/products')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="panel">
    <div class="panel-header">
      <h2>新建商品</h2>
      <div>
        <el-button @click="router.back()">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存草稿</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-form label-position="top">
      <section class="form-section">
        <h3>基础信息</h3>
        <p>类目、状态和库存粒度全部使用选择器，不需要手填 ID 或 JSON。</p>
        <el-form-item label="商品标题">
          <el-input v-model="form.title" maxlength="255" show-word-limit placeholder="例如：有机鲜牛奶 1L" />
        </el-form-item>
        <el-form-item label="商品描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="面向顾客的商品描述" />
        </el-form-item>
        <div class="policy-grid">
          <el-form-item label="类目">
            <el-select v-model="form.categoryId" style="width: 100%">
              <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width: 100%">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="上架" value="ACTIVE" />
            </el-select>
          </el-form-item>
          <el-form-item label="库存粒度">
            <el-select v-model="form.inventoryTrackingLevel" style="width: 100%">
              <el-option label="按规格（SKU）" value="VARIANT" />
              <el-option label="按商品（SPU）" value="PRODUCT" />
            </el-select>
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>规格</h3>
        <p>每行一个 SKU。规格名和规格值分开选择/输入，不使用逗号分隔字符串。</p>
        <div v-for="(variant, index) in form.variants" :key="index" class="variant-row">
          <el-input v-model="variant.skuCode" placeholder="SKU 编码" />
          <el-input v-model="variant.barcode" placeholder="条码（可选）" />
          <el-input-number v-model="variant.priceYuan" :min="0" :precision="2" :step="1" controls-position="right" />
          <el-select v-model="variant.currency">
            <el-option v-for="currency in currencies" :key="currency" :label="currency" :value="currency" />
          </el-select>
          <el-select v-model="variant.optionName">
            <el-option v-for="name in optionNames" :key="name" :label="name" :value="name" />
          </el-select>
          <el-input v-model="variant.optionValue" placeholder="规格值" />
          <el-button type="danger" text @click="removeVariant(index)">删除</el-button>
        </div>
        <el-button @click="addVariant">+ 添加规格</el-button>
        <p class="muted">价格以元输入，提交时自动转换为最小货币单位（分）。</p>
      </section>

      <section class="form-section">
        <h3>批次与保质期</h3>
        <p>保质期商品会按“SKU + 地点 + 批次 + 效期”管理库存。</p>
        <el-form-item label="启用批次/效期管理">
          <el-switch v-model="form.shelfLifeManaged" />
        </el-form-item>
        <div v-if="form.shelfLifeManaged" class="policy-grid">
          <el-form-item label="强制批次">
            <el-switch v-model="form.policy.batchTracking" />
          </el-form-item>
          <el-form-item label="拣货策略">
            <el-select v-model="form.policy.pickingStrategy" style="width: 100%">
              <el-option label="FEFO：先到期先出" value="FEFO" />
              <el-option label="FIFO：先进先出" value="FIFO" />
              <el-option label="NONE：不按批次" value="NONE" />
            </el-select>
          </el-form-item>
          <el-form-item label="最低剩余保质期（天）">
            <el-input-number v-model="form.policy.minRemainingShelfLifeDays" :min="0" :max="3650" />
          </el-form-item>
          <el-form-item label="临期阈值（天）">
            <el-input-number v-model="form.policy.nearExpiryThresholdDays" :min="0" :max="3650" />
          </el-form-item>
          <el-form-item label="临期动作">
            <el-select v-model="form.policy.nearExpiryAction" style="width: 100%">
              <el-option label="正常销售" value="NORMAL" />
              <el-option label="折扣清货" value="DISCOUNT" />
              <el-option label="下架" value="DELIST" />
              <el-option label="报损" value="WRITE_OFF" />
            </el-select>
          </el-form-item>
          <el-form-item label="过期自动阻断">
            <el-switch v-model="form.policy.autoBlockExpired" />
          </el-form-item>
        </div>
      </section>
    </el-form>
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}
</style>