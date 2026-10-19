<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type {
  CreatePromotionInput,
  GroupBuyView,
  Product,
  ProductPage,
  PromotionType,
  PromotionView
} from '@fullmall/api-client'
import { api } from '../api'

const promotions = ref<PromotionView[]>([])
const products = ref<Product[]>([])
const groupBuys = ref<GroupBuyView[]>([])
const loading = ref(false)
const saving = ref(false)
const error = ref('')
const dialog = ref(false)
const groupDialog = ref(false)
const selectedPromotion = ref<PromotionView | null>(null)

const form = reactive({
  code: '',
  title: '',
  description: '',
  type: 'FLASH_SALE' as PromotionType,
  skuId: '',
  window: [] as Date[],
  priceYuan: undefined as number | undefined,
  giftSkuId: '',
  buyQuantity: 2,
  giftQuantity: 1,
  maxGiftPerOrder: 2,
  stockLimit: 50,
  perUserLimit: 5,
  groupSize: 2,
  groupDurationHours: 24,
  priority: 0
})

const skuOptions = computed(() =>
  products.value.flatMap(product =>
    product.skus.map(sku => ({
      value: sku.id,
      label: `${product.title} · ${Object.values(sku.options).join(' / ') || sku.skuCode} · ${money(sku.price)}`,
      productTitle: product.title
    }))
  )
)

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [promotionList, productPage] = await Promise.all([
      api.get<PromotionView[]>('/admin/v1/promotions'),
      api.get<ProductPage>('/admin/v1/products', { locale: 'zh-CN', size: 100 })
    ])
    promotions.value = promotionList
    products.value = productPage.items
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '促销加载失败'
  } finally {
    loading.value = false
  }
}

function openCreate() {
  form.code = `PROMO-${Date.now().toString().slice(-6)}`
  form.title = ''
  form.description = ''
  form.type = 'FLASH_SALE'
  form.skuId = skuOptions.value[0]?.value ?? ''
  form.window = [new Date(), new Date(Date.now() + 7 * 24 * 3600 * 1000)]
  form.priceYuan = 29.9
  form.giftSkuId = ''
  form.buyQuantity = 2
  form.giftQuantity = 1
  form.maxGiftPerOrder = 2
  form.stockLimit = 50
  form.perUserLimit = 5
  form.groupSize = 2
  form.groupDurationHours = 24
  form.priority = 0
  dialog.value = true
}

async function submit() {
  error.value = ''
  if (!form.title.trim() || !form.skuId || form.window.length !== 2) {
    error.value = '请填写活动名称、商品和活动时间'
    return
  }
  if (form.type !== 'BUY_GIFT' && (!form.priceYuan || form.priceYuan <= 0)) {
    error.value = '请填写正确的活动价'
    return
  }
  if (form.type === 'BUY_GIFT' && !form.giftSkuId) {
    error.value = '请选择赠品'
    return
  }
  const payload: CreatePromotionInput = {
    code: form.code.trim(),
    type: form.type,
    title: form.title.trim(),
    description: form.description.trim() || undefined,
    startsAt: form.window[0].toISOString(),
    endsAt: form.window[1].toISOString(),
    skuId: form.skuId,
    priority: form.priority,
    promoPriceAmount: form.type === 'BUY_GIFT' ? undefined : Math.round((form.priceYuan ?? 0) * 100),
    giftSkuId: form.type === 'BUY_GIFT' ? form.giftSkuId : undefined,
    buyQuantity: form.type === 'BUY_GIFT' ? form.buyQuantity : undefined,
    giftQuantity: form.type === 'BUY_GIFT' ? form.giftQuantity : undefined,
    maxGiftPerOrder: form.type === 'BUY_GIFT' ? form.maxGiftPerOrder : undefined,
    stockLimit: form.type === 'FLASH_SALE' || form.type === 'BUY_GIFT' ? form.stockLimit : undefined,
    perUserLimit: form.perUserLimit,
    groupSize: form.type === 'GROUP_BUY' ? form.groupSize : undefined,
    groupDurationHours: form.type === 'GROUP_BUY' ? form.groupDurationHours : undefined
  }
  saving.value = true
  try {
    await api.post<PromotionView>('/admin/v1/promotions', payload)
    ElMessage.success('促销已创建并生效')
    dialog.value = false
    await load()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '创建促销失败'
  } finally {
    saving.value = false
  }
}

async function changeStatus(promotion: PromotionView, status: 'ACTIVE' | 'ENDED') {
  try {
    await api.post<PromotionView>(`/admin/v1/promotions/${promotion.id}/status?status=${status}`)
    ElMessage.success(status === 'ACTIVE' ? '活动已启用' : '活动已结束')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '操作失败')
  }
}

async function openGroupBuys(promotion: PromotionView) {
  selectedPromotion.value = promotion
  groupBuys.value = []
  groupDialog.value = true
  try {
    groupBuys.value = await api.get<GroupBuyView[]>('/admin/v1/promotions/group-buys', {
      promotionId: promotion.id
    })
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '拼团记录加载失败')
  }
}

function money(value?: { amount: number; currency: string }) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}

function typeLabel(type: PromotionType) {
  return { FLASH_SALE: '秒杀', BUY_GIFT: '买赠', GROUP_BUY: '拼团' }[type]
}

function statusLabel(status: string) {
  return { ACTIVE: '进行中', DRAFT: '草稿', ENDED: '已结束' }[status] ?? status
}

function windowLabel(promotion: PromotionView) {
  return `${new Date(promotion.startsAt).toLocaleString('zh-CN')} 至 ${new Date(promotion.endsAt).toLocaleString('zh-CN')}`
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>促销管理</h2>
        <p class="muted">秒杀、买赠、拼团统一配置；价格、限购和活动库存由服务端校验。</p>
      </div>
      <div class="header-actions">
        <el-button @click="load">刷新</el-button>
        <el-button type="primary" @click="openCreate">新建促销</el-button>
      </div>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-table :data="promotions" stripe>
      <el-table-column prop="title" label="活动" min-width="180">
        <template #default="{ row }">
          <strong>{{ row.title }}</strong>
          <div class="muted">{{ row.code }} · {{ windowLabel(row) }}</div>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'FLASH_SALE' ? 'danger' : row.type === 'GROUP_BUY' ? 'warning' : 'success'">
            {{ typeLabel(row.type) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="180">
        <template #default="{ row }">
          {{ row.productTitle }}
          <div class="muted">{{ row.skuCode }}</div>
        </template>
      </el-table-column>
      <el-table-column label="活动价" width="120">
        <template #default="{ row }">{{ money(row.promoPrice) }}</template>
      </el-table-column>
      <el-table-column label="规则" min-width="180">
        <template #default="{ row }">
          <span v-if="row.type === 'BUY_GIFT'">
            买 {{ row.buyQuantity }} 赠 {{ row.giftQuantity }} · {{ row.giftTitle }}
          </span>
          <span v-else-if="row.type === 'GROUP_BUY'">
            {{ row.groupSize }} 人成团 · {{ row.groupDurationHours }} 小时
          </span>
          <span v-else>
            已售 {{ row.soldQuantity }} / {{ row.stockLimit }}
            <template v-if="row.perUserLimit"> · 每人限 {{ row.perUserLimit }}</template>
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="row.active ? 'success' : row.status === 'ENDED' ? 'info' : 'warning'">
            {{ row.active ? '进行中' : statusLabel(row.status) }}
          </el-tag>
          <div v-if="row.soldOut" class="muted">已售罄</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="210">
        <template #default="{ row }">
          <el-button v-if="row.type === 'GROUP_BUY'" link @click="openGroupBuys(row)">拼团记录</el-button>
          <el-button v-if="row.status !== 'ACTIVE'" link type="success" @click="changeStatus(row, 'ACTIVE')">
            启用
          </el-button>
          <el-button v-if="row.status === 'ACTIVE'" link type="danger" @click="changeStatus(row, 'ENDED')">
            结束
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" title="新建促销" width="720px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="活动名称">
            <el-input v-model="form.title" maxlength="200" placeholder="例如：鲜奶买二赠一" />
          </el-form-item>
          <el-form-item label="活动类型">
            <el-select v-model="form.type">
              <el-option label="秒杀" value="FLASH_SALE" />
              <el-option label="买赠" value="BUY_GIFT" />
              <el-option label="拼团" value="GROUP_BUY" />
            </el-select>
          </el-form-item>
          <el-form-item label="活动编码">
            <el-input v-model="form.code" maxlength="64" />
          </el-form-item>
          <el-form-item label="优先级">
            <el-input-number v-model="form.priority" :min="0" :max="1000" />
          </el-form-item>
        </div>
        <el-form-item label="活动商品">
          <el-select v-model="form.skuId" filterable placeholder="选择商品规格">
            <el-option v-for="sku in skuOptions" :key="sku.value" :label="sku.label" :value="sku.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="活动时间">
          <el-date-picker
            v-model="form.window"
            type="datetimerange"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            style="width: 100%"
          />
        </el-form-item>
        <div class="form-grid">
          <el-form-item v-if="form.type !== 'BUY_GIFT'" label="活动价（元）">
            <el-input-number v-model="form.priceYuan" :min="0.01" :precision="2" :step="1" />
          </el-form-item>
          <el-form-item v-if="form.type === 'BUY_GIFT'" label="赠品">
            <el-select v-model="form.giftSkuId" filterable placeholder="选择赠品规格">
              <el-option v-for="sku in skuOptions" :key="sku.value" :label="sku.label" :value="sku.value" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="form.type === 'BUY_GIFT'" label="购买数量">
            <el-input-number v-model="form.buyQuantity" :min="1" />
          </el-form-item>
          <el-form-item v-if="form.type === 'BUY_GIFT'" label="赠送数量">
            <el-input-number v-model="form.giftQuantity" :min="1" />
          </el-form-item>
          <el-form-item v-if="form.type === 'BUY_GIFT'" label="每单最多赠">
            <el-input-number v-model="form.maxGiftPerOrder" :min="1" />
          </el-form-item>
          <el-form-item v-if="form.type === 'GROUP_BUY'" label="成团人数">
            <el-input-number v-model="form.groupSize" :min="2" />
          </el-form-item>
          <el-form-item v-if="form.type === 'GROUP_BUY'" label="拼团时长（小时）">
            <el-input-number v-model="form.groupDurationHours" :min="1" />
          </el-form-item>
          <el-form-item label="活动库存">
            <el-input-number v-model="form.stockLimit" :min="1" />
          </el-form-item>
          <el-form-item label="每人限购">
            <el-input-number v-model="form.perUserLimit" :min="1" />
          </el-form-item>
        </div>
        <el-form-item label="活动说明">
          <el-input v-model="form.description" type="textarea" :rows="2" maxlength="1000" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">创建并启用</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="groupDialog" title="拼团记录" size="620px">
      <p class="muted">{{ selectedPromotion?.title }}</p>
      <el-empty v-if="!groupBuys.length" description="暂无拼团记录" />
      <el-table v-else :data="groupBuys" size="small">
        <el-table-column prop="code" label="团号" width="110" />
        <el-table-column label="进度" width="120">
          <template #default="{ row }">{{ row.joinedCount }} / {{ row.groupSize }}（已付 {{ row.paidCount }}）</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'SUCCESS' ? 'success' : row.status === 'FAILED' ? 'info' : 'warning'">
              {{ row.status === 'SUCCESS' ? '已成团' : row.status === 'FAILED' ? '已失败' : '拼团中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成员">
          <template #default="{ row }">
            <span v-for="member in row.members" :key="member.maskedPhone + member.joinedAt" class="member-chip">
              {{ member.maskedPhone }}{{ member.paid ? ' · 已付' : '' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="截止" width="170">
          <template #default="{ row }">{{ new Date(row.expiresAt).toLocaleString('zh-CN') }}</template>
        </el-table-column>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped>
.header-actions {
  display: flex;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.member-chip {
  display: inline-block;
  margin: 2px 6px 2px 0;
  padding: 2px 8px;
  border-radius: 999px;
  background: #f2f5f2;
  font-size: 12px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>