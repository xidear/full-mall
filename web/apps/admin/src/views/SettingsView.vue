<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

const loading = ref(false)
const saving = ref(false)
const error = ref('')

const form = reactive<Record<string, string>>({
  site_name: '',
  company_name: '',
  icp_number: '',
  public_security_record: '',
  service_phone: '',
  service_email: '',
  site_status: 'OPEN',
  maintenance_message: '',
  default_locale: 'zh-CN',
  default_currency: 'CNY',
  storage_provider: 'mock',
  sms_provider: 'mock',
  payment_provider: 'mock',
  logistics_provider: 'mock',
  sms_access_key: '',
  sms_secret_key: '',
  sms_sign_name: '',
  logistics_key: '',
  logistics_customer: '',
  map_provider: '',
  map_key: '',
  // 分销
  'distribution.level1-mode': 'GROSS',
  'distribution.level1-value': '5',
  'distribution.level2-enabled': 'true',
  'distribution.level2-mode': 'GROSS',
  'distribution.level2-value': '2',
  'distribution.bind-mode': 'PERMANENT',
  'distribution.bind-timing': 'CART',
  'distribution.freeze-policy': 'AFTER_SALE_CLOSED',
  'distribution.after-sale-window-days': '7',
  'distribution.min-withdrawal': '10000',
  'distribution.payout-methods': 'ALIPAY,BANK',
  'distribution.staff-payout': 'PAYROLL',
  'distribution.staff-level2-enabled': 'false'
})

// 渠道自检：直接调用发送链路，返回真实结果（不做前端模拟）
const testChannel = ref('IN_APP')
const testing = ref(false)
interface TestResult { status: string; errorMessage?: string | null }
const testResult = ref<TestResult | null>(null)

async function runChannelTest() {
  if (testing.value) return
  testing.value = true
  testResult.value = null
  try {
    testResult.value = await api.post<TestResult>('/admin/v1/notifications/test-send', {
      channel: testChannel.value
    })
  } catch (cause) {
    testResult.value = {
      status: 'FAILED',
      errorMessage: cause instanceof Error ? cause.message : '发送失败'
    }
  } finally {
    testing.value = false
  }
}

const minWithdrawalYuan = computed({
  get: () => Number(form['distribution.min-withdrawal'] || 0) / 100,
  set: (value: number) => { form['distribution.min-withdrawal'] = String(Math.round(value * 100)) }
})

const payoutMethods = computed({
  get: () => (form['distribution.payout-methods'] || '').split(',').filter(Boolean),
  set: (value: string[]) => { form['distribution.payout-methods'] = value.join(',') }
})

onMounted(load)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const values = await api.get<Record<string, string>>('/admin/v1/settings')
    Object.assign(form, values)
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '设置加载失败'
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  error.value = ''
  try {
    const values = await api.put<Record<string, string>>('/admin/v1/settings', { ...form })
    Object.assign(form, values)
    ElMessage.success('系统设置已保存')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="panel" v-loading="loading">
    <div class="panel-header">
      <div>
        <h2>系统设置</h2>
        <p class="muted">大陆运营所需的备案信息、主体信息、客服信息和站点开关都在这里配置。</p>
      </div>
      <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
    </div>
    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />

    <el-form label-position="top">
      <section class="form-section">
        <h3>站点信息</h3>
        <div class="policy-grid">
          <el-form-item label="站点名称">
            <el-input v-model="form.site_name" />
          </el-form-item>
          <el-form-item label="公司主体">
            <el-input v-model="form.company_name" />
          </el-form-item>
          <el-form-item label="ICP备案号">
            <el-input v-model="form.icp_number" placeholder="如：京ICP备xxxxxxxx号-1" />
          </el-form-item>
          <el-form-item label="公网安备">
            <el-input v-model="form.public_security_record" placeholder="如：京公网安备 xxxxxxxxxxxxxx号" />
          </el-form-item>
          <el-form-item label="客服电话">
            <el-input v-model="form.service_phone" />
          </el-form-item>
          <el-form-item label="客服邮箱">
            <el-input v-model="form.service_email" />
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>分销设置</h3>
        <p class="muted">比例、绑定、冻结与提现规则都在这里配置，保存后立即生效。</p>
        <div class="policy-grid">
          <el-form-item label="一级佣金方式">
            <el-select v-model="form['distribution.level1-mode']" style="width: 100%">
              <el-option label="按订单金额百分比" value="GROSS" />
              <el-option label="固定金额（分）" value="FIXED" />
            </el-select>
          </el-form-item>
          <el-form-item label="一级佣金值">
            <el-input v-model="form['distribution.level1-value']"
                      :placeholder="form['distribution.level1-mode'] === 'GROSS' ? '5 表示 5%' : '500 表示 5.00 元'" />
          </el-form-item>
          <el-form-item label="开启二级分销">
            <el-switch v-model="form['distribution.level2-enabled']" active-value="true" inactive-value="false" />
          </el-form-item>
          <el-form-item label="二级佣金方式">
            <el-select v-model="form['distribution.level2-mode']" style="width: 100%">
              <el-option label="按订单金额百分比" value="GROSS" />
              <el-option label="固定金额（分）" value="FIXED" />
            </el-select>
          </el-form-item>
          <el-form-item label="二级佣金值">
            <el-input v-model="form['distribution.level2-value']" />
          </el-form-item>
          <el-form-item label="绑定方式">
            <el-select v-model="form['distribution.bind-mode']" style="width: 100%">
              <el-option label="一次性绑定（先到先得）" value="ONCE" />
              <el-option label="终身绑定（可被接管）" value="PERMANENT" />
            </el-select>
          </el-form-item>
          <el-form-item label="绑定时机">
            <el-select v-model="form['distribution.bind-timing']" style="width: 100%">
              <el-option label="注册时" value="REGISTER" />
              <el-option label="下单时" value="CART" />
              <el-option label="访问时" value="VISIT" />
            </el-select>
          </el-form-item>
          <el-form-item label="冻结策略">
            <el-select v-model="form['distribution.freeze-policy']" style="width: 100%">
              <el-option label="订单完成 + 售后窗口（推荐）" value="AFTER_SALE_CLOSED" />
              <el-option label="固定天数" value="FIXED_DAYS" />
            </el-select>
          </el-form-item>
          <el-form-item label="售后窗口天数">
            <el-input v-model="form['distribution.after-sale-window-days']" />
          </el-form-item>
          <el-form-item label="最低提现（元）">
            <el-input-number v-model="minWithdrawalYuan" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
          <el-form-item label="可选打款方式">
            <el-select v-model="payoutMethods" multiple style="width: 100%">
              <el-option label="支付宝" value="ALIPAY" />
              <el-option label="银行卡" value="BANK" />
              <el-option label="微信零钱" value="WECHAT" />
            </el-select>
          </el-form-item>
          <el-form-item label="员工佣金出款通道">
            <el-select v-model="form['distribution.staff-payout']" style="width: 100%">
              <el-option label="工资 / 绩效（不进钱包）" value="PAYROLL" />
              <el-option label="也进钱包（可提现）" value="WALLET" />
            </el-select>
          </el-form-item>
          <el-form-item label="员工二级（店长带队）">
            <el-switch v-model="form['distribution.staff-level2-enabled']"
                       active-value="true" inactive-value="false" />
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>站点状态</h3>
        <div class="policy-grid">
          <el-form-item label="营业状态">
            <el-select v-model="form.site_status" style="width: 100%">
              <el-option label="正常营业" value="OPEN" />
              <el-option label="维护中" value="MAINTENANCE" />
              <el-option label="暂停营业" value="CLOSED" />
            </el-select>
          </el-form-item>
          <el-form-item label="维护提示">
            <el-input v-model="form.maintenance_message" />
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>市场与适配器</h3>
        <div class="policy-grid">
          <el-form-item label="默认语言">
            <el-select v-model="form.default_locale" style="width: 100%">
              <el-option label="简体中文" value="zh-CN" />
              <el-option label="繁体中文（香港）" value="zh-HK" />
              <el-option label="繁体中文（台湾）" value="zh-TW" />
              <el-option label="English" value="en" />
              <el-option label="Español" value="es" />
              <el-option label="Français" value="fr" />
              <el-option label="العربية" value="ar" />
            </el-select>
          </el-form-item>
          <el-form-item label="默认币种">
            <el-select v-model="form.default_currency" style="width: 100%">
              <el-option label="CNY 人民币" value="CNY" />
              <el-option label="HKD 港币" value="HKD" />
              <el-option label="MOP 澳门元" value="MOP" />
              <el-option label="TWD 新台币" value="TWD" />
              <el-option label="USD 美元" value="USD" />
              <el-option label="EUR 欧元" value="EUR" />
            </el-select>
          </el-form-item>
          <el-form-item label="存储适配器">
            <el-select v-model="form.storage_provider" style="width: 100%">
              <el-option label="Mock（开源默认）" value="mock" />
              <el-option label="阿里云 OSS（待接入）" value="aliyun-oss" />
              <el-option label="七牛云（待接入）" value="qiniu" />
              <el-option label="S3 / SeaweedFS" value="s3" />
            </el-select>
          </el-form-item>
          <el-form-item label="短信适配器">
            <el-select v-model="form.sms_provider" style="width: 100%">
              <el-option label="Mock（开源默认）" value="mock" />
              <el-option label="阿里云短信（待接入）" value="aliyun" />
              <el-option label="腾讯云短信（待接入）" value="tencent" />
            </el-select>
          </el-form-item>
          <el-form-item label="支付适配器">
            <el-select v-model="form.payment_provider" style="width: 100%">
              <el-option label="Mock（开源默认）" value="mock" />
              <el-option label="微信支付（待接入）" value="wechat" />
              <el-option label="支付宝（待接入）" value="alipay" />
            </el-select>
          </el-form-item>
          <el-form-item label="物流适配器">
            <el-select v-model="form.logistics_provider" style="width: 100%">
              <el-option label="Mock（开源默认）" value="mock" />
              <el-option label="顺丰（待接入）" value="sf" />
              <el-option label="京东物流（待接入）" value="jd" />
            </el-select>
          </el-form-item>
        </div>
        <p class="muted">真实适配器需要在环境变量或密钥管理中配置凭证；开源默认 Mock，不访问外部服务。</p>

        <h3 style="margin-top: 16px">渠道自检</h3>
        <div class="policy-grid">
          <el-form-item label="测试渠道">
            <el-select v-model="testChannel" style="width: 100%">
              <el-option label="站内消息" value="IN_APP" />
              <el-option label="短信" value="SMS" />
              <el-option label="邮件" value="EMAIL" />
              <el-option label="微信" value="WECHAT" />
              <el-option label="APP 推送" value="APP" />
            </el-select>
          </el-form-item>
          <el-form-item label="操作">
            <el-button :loading="testing" @click="runChannelTest">发送测试消息</el-button>
          </el-form-item>
        </div>
        <el-alert v-if="testResult" :type="testResult.status === 'SENT' ? 'success' : 'error'"
                  show-icon :closable="false"
                  :title="testResult.status === 'SENT'
                    ? '渠道可用：测试消息已发送'
                    : `渠道不可用：${testResult.errorMessage ?? '发送失败'}`" />
      </section>
    </el-form>
  </div>
</template>

<style scoped>
.mb-16 {
  margin-bottom: 16px;
}

.form-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #ece9e2;
}

.form-section h3 {
  margin: 0 0 12px;
}
</style>