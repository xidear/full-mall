<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ProductUpsertRequest } from '@fullmall/api-client'
import { api } from '../api'
import AssetPicker from '../components/AssetPicker.vue'

const router = useRouter()
const route = useRoute()
// 编辑态才有商品 id；新建态禁用 AI 生成入口（生成需要商品上下文留痕）
const productId = String(route.params.id ?? route.query.productId ?? '')
const editing = Boolean(route.params.id)
const mediaList = ref<Array<{ id: string; role: string; skuId?: string | null; assetId: string; url: string }>>([])
const assetOptions = ref<Array<{ id: string; name: string; url: string; kind: string }>>([])
const hasImageProvider = ref(false)
const mediaForm = ref({ assetId: '', role: 'MAIN', skuId: '', sortOrder: 0 })

async function loadExisting() {
  if (!editing) return
  try {
    const product = await api.get<{
      title: string; description?: string; categoryId?: string
      skus?: Array<{ skuCode: string; barcode?: string; imageUrl?: string
        price: { amount: number; currency: string }; options?: Record<string, string> }>
    }>(`/admin/v1/products/${productId}`)
    form.title = product.title
    form.description = product.description ?? ''
    if (product.categoryId) form.categoryId = product.categoryId
    if (product.skus?.length) {
      form.variants = product.skus.map(sku => ({
        skuCode: sku.skuCode,
        barcode: sku.barcode ?? '',
        imageUrl: sku.imageUrl ?? '',
        priceYuan: sku.price.amount / 100,
        currency: sku.price.currency,
        optionName: Object.keys(sku.options ?? {})[0] ?? '规格',
        optionValue: Object.values(sku.options ?? {})[0] ?? ''
      }))
    }
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '加载商品失败')
  }
}

async function loadMediaContext() {
  try {
    const providers = await api.get<Array<{ kind: string }>>('/admin/v1/ai/media/providers')
    hasImageProvider.value = providers.some(p => p.kind === 'IMAGE')
    const page = await api.get<{ content: Array<{ id: string; name: string; url: string; kind: string }> }>(
      '/admin/v1/assets?size=100&sort=createdAt,desc')
    assetOptions.value = page.content ?? []
    if (productId) {
      mediaList.value = await api.get(`/admin/v1/products/${productId}/media`)
    }
  } catch {
    // 供应商/素材拉取失败不阻断商品编辑，但按钮会保持禁用并有提示
    hasImageProvider.value = false
  }
}

loadExisting()
void loadMediaContext()

async function attachMedia() {
  if (!productId) { ElMessage.error('先保存商品后再挂素材'); return }
  if (!mediaForm.value.assetId) { ElMessage.error('先选择素材'); return }
  try {
    await api.post(`/admin/v1/products/${productId}/media`, {
      assetId: mediaForm.value.assetId,
      role: mediaForm.value.role,
      skuId: mediaForm.value.role === 'SPEC' ? (mediaForm.value.skuId || null) : null,
      sortOrder: mediaForm.value.sortOrder,
      source: 'UPLOAD'
    })
    mediaList.value = await api.get(`/admin/v1/products/${productId}/media`)
    ElMessage.success('已挂到商品')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '挂载失败')
  }
}

async function detachMedia(id: string) {
  try {
    await api.delete(`/admin/v1/products/${productId}/media/${id}`)
    mediaList.value = await api.get(`/admin/v1/products/${productId}/media`)
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '移除失败')
  }
}
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
  imageUrl: string
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
      imageUrl: '',
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
    imageUrl: '',
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

function openMaterials() {
  router.push('/materials')
}

function openAiMedia() {
  // 带上商品上下文：生成的提示词与导图会留痕到该商品，回到此页复制 URL 填到规格图
  router.push({ path: '/ai-media', query: { productId: productId } })
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
      imageUrl: variant.imageUrl?.trim() || undefined,
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
    if (editing) {
      await api.put(`/admin/v1/products/${productId}`, {
        title: payload.title,
        description: payload.description,
        categoryId: form.categoryId || null
      })
      ElMessage.success('已保存')
      return
    }
    await api.post('/admin/v1/products', payload)
    ElMessage.success('商品已创建')
    await router.push('/products')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    saving.value = false
  }
}

loadMediaContext()
</script>

<template>
  <div class="panel">
    <div class="panel-header">
      <h2>{{ editing ? '编辑商品' : '新建商品' }}</h2>
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
        <h3>商品素材（主图 / 图集 / 详情图 / 视频）</h3>
        <p class="muted">素材只从素材库选择；规格专用图请用「规格图」并在规格区挂到具体 SKU。</p>
        <div class="media-grid">
          <div v-for="item in mediaList" :key="item.id" class="media-card">
            <img :src="item.url" :alt="item.role" />
            <div class="media-meta">
              <el-tag size="small">{{ item.role }}</el-tag>
              <el-button text type="danger" size="small" @click="detachMedia(item.id)">移除</el-button>
            </div>
          </div>
          <div class="media-card empty">
            <AssetPicker v-model="mediaForm.assetId" label="选择素材" />
            <el-select v-model="mediaForm.role" style="width: 100%; margin-top: 6px">
              <el-option label="主图" value="MAIN" />
              <el-option label="图集" value="GALLERY" />
              <el-option label="详情图" value="DETAIL" />
              <el-option label="视频" value="VIDEO" />
            </el-select>
            <el-button style="margin-top: 6px" :disabled="!productId" @click="attachMedia">挂到商品</el-button>
            <el-button text type="primary" @click="openAiMedia"
                       :disabled="!productId || !hasImageProvider">
              主图不够？AI 生成
            </el-button>
            <span v-if="!hasImageProvider" class="muted">未配置图片生成供应商</span>
          </div>
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
          <div class="image-cell">
            <AssetPicker v-model="variant.imageUrl" label="选择规格图" :value-type="'url'" />
            <el-button text type="primary" :disabled="!productId || !hasImageProvider" @click="openAiMedia">AI 生成主图</el-button>
          </div>
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

.media-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(150px, 1fr)); gap: 12px; }
.media-card { border: 1px solid #eee; border-radius: 6px; padding: 8px; display: flex; flex-direction: column; gap: 6px; }
.media-card img { width: 100%; height: 110px; object-fit: cover; border-radius: 4px; }
.media-card.empty { justify-content: center; }
.media-meta { display: flex; align-items: center; justify-content: space-between; }
</style>