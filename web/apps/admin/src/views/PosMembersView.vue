<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { MemberAdminView, MemberLevel, PosCoupon, PosMember } from '@fullmall/api-client'
import { api } from '../api'

const activeTab = ref<'members' | 'coupons' | 'levels'>('members')
const members = ref<MemberAdminView[]>([])
const coupons = ref<PosCoupon[]>([])
const levels = ref<MemberLevel[]>([])
const search = ref('')
const loading = ref(false)
const error = ref('')

const memberDialog = ref(false)
const memberForm = reactive({ name: '', phone: '', tier: 'STANDARD', birthday: '' })

const topUpDialog = ref(false)
const topUpTarget = ref<PosMember | null>(null)
const topUpForm = reactive({ amount: 100, reason: '门店会员充值' })

const pointsDialog = ref(false)
const pointsTarget = ref<PosMember | null>(null)
const pointsForm = reactive({ points: 100, reason: '门店积分调整' })

const couponDialog = ref(false)
const couponForm = reactive({
  code: '',
  name: '',
  discountType: 'FIXED',
  discountValue: 10,
  minSpend: 100,
  maxDiscount: 30,
  memberId: '',
  validFrom: new Date(),
  validTo: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
})

const tierLabels: Record<string, string> = {
  STANDARD: '普通会员',
  SILVER: '银卡会员',
  GOLD: '金卡会员',
  PLATINUM: '白金会员'
}

const couponStatusLabels: Record<string, string> = {
  ISSUED: '未使用',
  LOCKED: '收银锁定',
  USED: '已使用',
  EXPIRED: '已过期'
}

const memberOptions = computed(() => members.value.map(row => ({
  value: row.member.id,
  label: `${row.member.name} · ${row.member.phone}`
})))

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  error.value = ''
  try {
    await Promise.all([loadMembers(), loadCoupons(), loadLevels()])
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '加载失败'
  } finally {
    loading.value = false
  }
}

async function loadMembers() {
  members.value = await api.get<MemberAdminView[]>('/admin/v1/pos/members', {
    query: search.value.trim() || undefined
  })
}

async function loadCoupons() {
  coupons.value = await api.get<PosCoupon[]>('/admin/v1/pos/coupons')
}

async function loadLevels() {
  levels.value = await api.get<MemberLevel[]>('/admin/v1/pos/member-levels')
}

async function saveLevel(level: MemberLevel) {
  loading.value = true
  try {
    await api.put(`/admin/v1/pos/member-levels/${level.code}`, {
      discountRate: level.discountRate,
      pointsMultiplier: level.pointsMultiplier,
      birthdayPoints: level.birthdayPoints
    })
    ElMessage.success(`${level.name} 已保存`)
    await loadLevels()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '会员等级保存失败'
  } finally {
    loading.value = false
  }
}

async function createMember() {
  if (!memberForm.name.trim() || !memberForm.phone.trim()) {
    ElMessage.warning('请填写姓名和手机号')
    return
  }
  loading.value = true
  try {
    await api.post<PosMember>('/admin/v1/pos/members', {
      name: memberForm.name.trim(),
      phone: memberForm.phone.trim(),
      tier: memberForm.tier,
      birthday: memberForm.birthday || undefined
    })
    memberDialog.value = false
    memberForm.name = ''
    memberForm.phone = ''
    memberForm.tier = 'STANDARD'
    await loadMembers()
    ElMessage.success('会员已创建')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '创建会员失败'
  } finally {
    loading.value = false
  }
}

function openTopUp(member: PosMember) {
  topUpTarget.value = member
  topUpForm.amount = 100
  topUpForm.reason = '门店会员充值'
  topUpDialog.value = true
}

async function submitTopUp() {
  if (!topUpTarget.value) return
  loading.value = true
  try {
    await api.post(`/admin/v1/pos/members/${topUpTarget.value.id}/stored-value`, {
      amount: yuanToFen(topUpForm.amount),
      reason: topUpForm.reason.trim()
    })
    topUpDialog.value = false
    await loadMembers()
    ElMessage.success('储值充值完成')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '充值失败'
  } finally {
    loading.value = false
  }
}

function openPoints(member: PosMember) {
  pointsTarget.value = member
  pointsForm.points = 100
  pointsForm.reason = '门店积分调整'
  pointsDialog.value = true
}

async function submitPoints() {
  if (!pointsTarget.value) return
  loading.value = true
  try {
    await api.post(`/admin/v1/pos/members/${pointsTarget.value.id}/points`, {
      points: Math.trunc(pointsForm.points),
      reason: pointsForm.reason.trim()
    })
    pointsDialog.value = false
    await loadMembers()
    ElMessage.success('积分已调整')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '积分调整失败'
  } finally {
    loading.value = false
  }
}

function openCouponDialog() {
  couponForm.code = ''
  couponForm.name = ''
  couponForm.discountType = 'FIXED'
  couponForm.discountValue = 10
  couponForm.minSpend = 100
  couponForm.maxDiscount = 30
  couponForm.memberId = ''
  couponForm.validFrom = new Date()
  couponForm.validTo = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000)
  couponDialog.value = true
}

async function createCoupon() {
  if (!couponForm.code.trim() || !couponForm.name.trim()) {
    ElMessage.warning('请填写优惠券编码和名称')
    return
  }
  loading.value = true
  try {
    await api.post<PosCoupon>('/admin/v1/pos/coupons', {
      code: couponForm.code.trim().toUpperCase(),
      name: couponForm.name.trim(),
      discountType: couponForm.discountType,
      discountValue: couponForm.discountType === 'PERCENT'
        ? Math.round(couponForm.discountValue)
        : yuanToFen(couponForm.discountValue),
      minSpendAmount: yuanToFen(couponForm.minSpend),
      maxDiscountAmount: couponForm.discountType === 'PERCENT' && couponForm.maxDiscount > 0
        ? yuanToFen(couponForm.maxDiscount)
        : undefined,
      memberId: couponForm.memberId || undefined,
      validFrom: couponForm.validFrom.toISOString(),
      validTo: couponForm.validTo.toISOString(),
      currency: 'CNY'
    })
    couponDialog.value = false
    await loadCoupons()
    ElMessage.success('优惠券已创建')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '创建优惠券失败'
  } finally {
    loading.value = false
  }
}

async function searchMembers() {
  loading.value = true
  try {
    await loadMembers()
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '会员搜索失败'
  } finally {
    loading.value = false
  }
}

function money(value?: { amount: number; currency: string }) {
  if (!value) return '-'
  return new Intl.NumberFormat('zh-CN', { style: 'currency', currency: value.currency }).format(value.amount / 100)
}

function couponValue(coupon: PosCoupon) {
  return coupon.discountType === 'PERCENT'
    ? `${coupon.discountValue}%`
    : money({ amount: coupon.discountValue, currency: coupon.currency || 'CNY' })
}

function yuanToFen(value: number) {
  return Math.round(Number(value || 0) * 100)
}
</script>

<template>
  <div v-loading="loading">
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="会员管理" name="members">
          <div class="toolbar-row">
            <el-input
              v-model="search"
              placeholder="会员编号 / 手机号 / 姓名"
              clearable
              style="width: 280px"
              @keyup.enter="searchMembers"
              @clear="searchMembers"
            />
            <el-button @click="searchMembers">搜索</el-button>
            <el-button type="primary" @click="memberDialog = true">新建会员</el-button>
            <el-button @click="loadAll">刷新</el-button>
          </div>

          <el-table :data="members" empty-text="暂无会员">
            <el-table-column prop="member.memberNo" label="会员编号" width="140" />
            <el-table-column prop="member.name" label="姓名" width="120" />
            <el-table-column prop="member.phone" label="手机号" width="140" />
            <el-table-column label="等级" width="110">
              <template #default="{ row }">{{ tierLabels[row.member.tier] || row.member.tier }}</template>
            </el-table-column>
            <el-table-column label="积分" width="100">
              <template #default="{ row }">{{ row.member.points }}</template>
            </el-table-column>
            <el-table-column label="成长值" width="110">
              <template #default="{ row }">{{ row.member.growthValue }}</template>
            </el-table-column>
            <el-table-column label="储值" width="120">
              <template #default="{ row }">{{ money(row.member.storedValue) }}</template>
            </el-table-column>
            <el-table-column prop="saleCount" label="消费笔数" width="100" />
            <el-table-column label="净消费" width="120">
              <template #default="{ row }">{{ money(row.totalSpent) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="190">
              <template #default="{ row }">
                <el-button size="small" @click="openTopUp(row.member)">充值</el-button>
                <el-button size="small" @click="openPoints(row.member)">积分</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="会员等级" name="levels">
          <el-alert
            title="等级折扣在收银绑定会员时自动生效；成长值按实付 1 元 = 1 成长值累计，达到门槛自动升级。"
            type="info"
            :closable="false"
            class="mb-16"
          />
          <el-table :data="levels" empty-text="暂无会员等级">
            <el-table-column prop="name" label="等级" width="140" />
            <el-table-column prop="code" label="编码" width="130" />
            <el-table-column label="折扣率（100=原价）" width="190">
              <template #default="{ row }">
                <el-input-number v-model="row.discountRate" :min="1" :max="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="积分倍率" width="140">
              <template #default="{ row }">
                <el-input-number v-model="row.pointsMultiplier" :min="1" :max="10" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="生日礼积分" width="160">
              <template #default="{ row }">
                <el-input-number v-model="row.birthdayPoints" :min="0" :step="100" size="small" />
              </template>
            </el-table-column>
            <el-table-column prop="minGrowth" label="升级成长值" width="130" />
            <el-table-column label="操作" width="110">
              <template #default="{ row }">
                <el-button size="small" type="primary" @click="saveLevel(row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="优惠券" name="coupons">
          <div class="toolbar-row">
            <el-button type="primary" @click="openCouponDialog">新建优惠券</el-button>
            <el-button @click="loadCoupons">刷新</el-button>
          </div>

          <el-table :data="coupons" empty-text="暂无优惠券">
            <el-table-column prop="code" label="编码" width="130" />
            <el-table-column prop="name" label="名称" min-width="190" />
            <el-table-column label="优惠" width="150">
              <template #default="{ row }">
                {{ couponValue(row) }}
                <span v-if="row.minSpend.amount > 0" class="muted">（满 {{ money(row.minSpend) }}）</span>
              </template>
            </el-table-column>
            <el-table-column label="最高优惠" width="120">
              <template #default="{ row }">{{ row.maxDiscount ? money(row.maxDiscount) : '—' }}</template>
            </el-table-column>
            <el-table-column label="限定会员" width="140">
              <template #default="{ row }">
                {{ row.memberId ? memberOptions.find(option => option.value === row.memberId)?.label || '指定会员' : '全场可用' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ISSUED' ? 'success' : row.status === 'LOCKED' ? 'warning' : 'info'">
                  {{ couponStatusLabels[row.status] || row.status }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="有效期至" width="180">
              <template #default="{ row }">{{ new Date(row.validTo).toLocaleString('zh-CN') }}</template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="memberDialog" title="新建会员" width="460px">
      <el-form label-position="top">
        <el-form-item label="姓名">
          <el-input v-model="memberForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="memberForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="会员等级">
          <el-select v-model="memberForm.tier" style="width: 100%">
            <el-option v-for="(label, value) in tierLabels" :key="value" :label="label" :value="value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生日（用于生日礼）">
          <el-date-picker v-model="memberForm.birthday" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="memberDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="createMember">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="topUpDialog" title="会员储值充值" width="460px">
      <el-form label-position="top">
        <el-form-item label="会员">
          <el-input :model-value="topUpTarget ? `${topUpTarget.name} · ${topUpTarget.phone}` : ''" disabled />
        </el-form-item>
        <el-form-item label="充值金额（元）">
          <el-input-number v-model="topUpForm.amount" :min="0.01" :precision="2" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="topUpForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="topUpDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitTopUp">确认充值</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pointsDialog" title="会员积分调整" width="460px">
      <el-form label-position="top">
        <el-form-item label="会员">
          <el-input :model-value="pointsTarget ? `${pointsTarget.name} · 当前 ${pointsTarget.points} 积分` : ''" disabled />
        </el-form-item>
        <el-form-item label="调整积分（正数增加，负数扣减）">
          <el-input-number v-model="pointsForm.points" :step="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原因">
          <el-input v-model="pointsForm.reason" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pointsDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="submitPoints">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="couponDialog" title="新建优惠券" width="560px">
      <el-form label-position="top">
        <el-form-item label="券编码">
          <el-input v-model="couponForm.code" placeholder="例如 POS20" />
        </el-form-item>
        <el-form-item label="券名称">
          <el-input v-model="couponForm.name" placeholder="例如 满 200 减 20" />
        </el-form-item>
        <el-form-item label="优惠类型">
          <el-radio-group v-model="couponForm.discountType">
            <el-radio-button label="FIXED">固定金额</el-radio-button>
            <el-radio-button label="PERCENT">折扣百分比</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item :label="couponForm.discountType === 'PERCENT' ? '折扣百分比（如 20 表示 8 折）' : '优惠金额（元）'">
          <el-input-number v-model="couponForm.discountValue" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="使用门槛（元）">
          <el-input-number v-model="couponForm.minSpend" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="couponForm.discountType === 'PERCENT'" label="最高优惠（元，0 表示不限）">
          <el-input-number v-model="couponForm.maxDiscount" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="限定会员（可空）">
          <el-select v-model="couponForm.memberId" clearable filterable style="width: 100%" placeholder="不限定会员">
            <el-option v-for="option in memberOptions" :key="option.value" :label="option.label" :value="option.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker v-model="couponForm.validFrom" type="datetime" style="width: 100%" />
        </el-form-item>
        <el-form-item label="失效时间">
          <el-date-picker v-model="couponForm.validTo" type="datetime" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="couponDialog = false">取消</el-button>
        <el-button type="primary" :loading="loading" @click="createCoupon">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.toolbar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}
</style>