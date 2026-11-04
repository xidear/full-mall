<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { VALIDATION, toMajor, toMinor } from '@fullmall/api-client'
import { api } from '../api'

const loading = ref(false)
const saving = ref(false)
const error = ref('')

// 时区/运营地区都必须是可选而不是手填（AGENTS §13）：
// 时区取浏览器内置 IANA 清单（与市场配置同一来源），运营地区取后台地区数据。
const timezoneOptions = ref<string[]>([])
const countryOptions = ref<Array<{ code: string; name: string }>>([])

interface AdapterOption {
  code: string
  label: string
  kind: 'MOCK' | 'BUILTIN' | 'CONFIGURED'
  available: boolean
  note: string
}
/** 适配器候选：只列"现在真的能用"的；未配置凭据的置灰并说明原因（不让选了之后运行期炸）。 */
const adapters = ref<Record<string, AdapterOption[]>>({})

function adapterOptions(capability: string): AdapterOption[] {
  return adapters.value[capability] ?? []
}

function adapterHint(capability: string, current: string): string {
  const options = adapterOptions(capability)
  if (options.length === 0) {
    return '候选加载失败，保存前请刷新'
  }
  const matched = options.find(item => item.code === current)
  if (!matched) {
    return '当前值 ' + current + ' 不在可用候选里：请重新选择后再保存'
  }
  return matched.available ? matched.note : '已选：' + matched.note
}

async function loadOptions() {
  const intlWithValues = Intl as typeof Intl & { supportedValuesOf?: (key: string) => string[] }
  timezoneOptions.value = intlWithValues.supportedValuesOf?.('timeZone') ?? []
  if (timezoneOptions.value.length === 0) {
    error.value = '当前浏览器不支持 Intl.supportedValuesOf，时区无法给出候选；请使用新版 Chrome/Edge/Safari'
  }
  try {
    countryOptions.value = await api.get<Array<{ code: string; name: string }>>(
      '/api/v1/regions/countries', { locale: 'zh-CN' })
    adapters.value = await api.get<Record<string, AdapterOption[]>>('/admin/v1/settings/adapters')
  } catch (cause) {
    error.value = '候选加载失败（运营地区/适配器）：' + (cause as Error).message
  }
}

interface EffectiveSetting {
  key: string
  label: string
  globalValue?: string
  marketValue?: string
  effectiveValue?: string
  source: 'MARKET' | 'GLOBAL'
}

/**
 * 市场级覆盖的生效值：同一项在「系统设置」和「市场配置」都能填，
 * 运营看不出最终用的是哪个。这里把 全局值 / 市场值 / 生效值 / 来源 摊开显示。
 */
const effective = ref<EffectiveSetting[]>([])
const effectiveMarket = ref('CN')

async function loadEffective(marketCode?: string) {
  // 不编造默认市场：没有默认市场时明确提示，而不是拿 CN 冒充
  const target = marketCode ?? form.default_market ?? ''
  if (!target) {
    effective.value = []
    ElMessage.warning('尚未设置默认市场：无法计算市场级生效值，请在下方先选择运营地区')
    return
  }
  try {
    effective.value = await api.get<EffectiveSetting[]>('/admin/v1/settings/effective', { market: target })
    effectiveMarket.value = target
  } catch (cause) {
    ElMessage.error('生效值加载失败：' + (cause instanceof Error ? cause.message : ''))
  }
}

const form = reactive<Record<string, string>>({
  site_name: '',
  site_tagline: '',
  default_market: 'CN',
  'app.timezone': 'Asia/Shanghai',
  media_public_base_url: '',
  branding_attribution: 'SHOW',
  site_domain: '',
  site_scheme: 'https',
  company_name: '',
  business_license_no: '',
  icp_number: '',
  public_security_record: '',
  service_phone: '',
  service_email: '',
  site_status: 'OPEN',
  maintenance_message: '',
  default_locale: 'zh-CN',
  enabled_locales: 'en,ja',
  default_currency: 'CNY',
  // 存储默认就是真实本地磁盘；"mock" 只是 ConfigurableAssetStorage 里的历史别名，新装站点不再写它
  storage_provider: 'local',
  payment_provider: 'mock',
  logistics_provider: 'mock',
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
  'distribution.staff-level2-enabled': 'false',
  // 素材上传上限（字节）。默认值镜像 docs/validation.schema.json；
  // 保存后立即对下一次上传生效，不需要重启后端。
  'upload.admin-image-max-bytes': String(VALIDATION.assetUpload.admin.maxBytes),
  'upload.admin-video-max-bytes': String(VALIDATION.assetUpload.adminVideo.maxBytes),
  'upload.member-image-max-bytes': String(VALIDATION.assetUpload.member.maxBytes),
  'upload.member-video-max-bytes': String(VALIDATION.assetUpload.memberVideo.maxBytes)
})

/** 库/接口里存字节，运营按 MB 填：这里只做单位换算，不做校验（范围交给后端）。 */
function megabytesField(key: string) {
  return computed({
    get: () => Number(form[key] || 0) / 1024 / 1024,
    set: (value: number) => { form[key] = String(Math.round(value * 1024 * 1024)) }
  })
}

const adminImageMb = megabytesField('upload.admin-image-max-bytes')
const adminVideoMb = megabytesField('upload.admin-video-max-bytes')
const memberImageMb = megabytesField('upload.member-image-max-bytes')
const memberVideoMb = megabytesField('upload.member-video-max-bytes')
const multipartCeilingMb = VALIDATION.assetUpload.multipartCeilingBytes / 1024 / 1024

// 渠道自检：直接调用发送链路，返回真实结果（不做前端模拟）
const testChannel = ref('IN_APP')
const testing = ref(false)
interface TestResult { status: string; errorMessage?: string | null }
const testResult = ref<TestResult | null>(null)

// 渠道自检打的是真实发送链路（后端不模拟）：演示环境会真的发出短信/邮件，注意配额与费用。
// 失败结果写进 testResult 并在页面显示原因，而不是只弹 toast——运维要能看到具体错误。
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

const siteUrlPreview = computed(() => {
  const raw = (form.site_domain || '').trim()
  if (!raw) return ''
  const port = raw.includes(':') ? raw.slice(raw.lastIndexOf(':') + 1) : ''
  const scheme = form.site_scheme === 'http' ? 'http'
    : form.site_scheme === 'https' ? 'https'
      : (!port || port === '443' ? 'https' : 'http')
  return scheme + '://' + raw
})

// 分 ↔ 元 的唯一转换点（设置项存分，输入框显示元）。边界：输入框被清空或输入非数字时
// Number() 得到 NaN，会被写成 "NaN" 存库；改这里时要保证非法输入有兜底或拦截。
const minWithdrawalYuan = computed({
  // 提现门槛是 CNY 金额（设置项存分）；按币种小数位换算，禁止写死 /100
  get: () => toMajor(Number(form['distribution.min-withdrawal'] || 0), 'CNY'),
  set: (value: number) => { form['distribution.min-withdrawal'] = String(toMinor(value, 'CNY')) }
})

const payoutMethods = computed({
  get: () => (form['distribution.payout-methods'] || '').split(',').filter(Boolean),
  set: (value: string[]) => { form['distribution.payout-methods'] = value.join(',') }
})

onMounted(async () => {
  await loadOptions()
  await load()
  await loadEffective()
})

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
    // 保存提交整份 form（含未改动的默认值）：新装站点因此会把这些键写成前端默认值（如 storage_provider=mock）。
    // 改 form 的默认值等于改“未配置键的落库值”，不只是改界面预填。
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
          <el-form-item label="站点副标题">
            <el-input v-model="form.site_tagline" placeholder="如：全品类商城 / 跨境好物" />
            <span class="muted">显示在页脚品牌行；留空则只显示站点名称。</span>
          </el-form-item>
          <el-form-item label="运营地区">
            <el-select v-model="form.default_market" filterable style="width: 100%" placeholder="选择运营地区">
              <el-option v-for="country in countryOptions" :key="country.code"
                         :label="country.name + '（' + country.code + '）'" :value="country.code" />
            </el-select>
            <span class="muted">决定法律文档、登录号码提示与备案信息是否展示，不再写死在页面里。</span>
          </el-form-item>
          <el-form-item label="站点默认时区">
            <el-select v-model="form['app.timezone']" filterable style="width: 100%" placeholder="选择时区（IANA）">
              <el-option v-for="zone in timezoneOptions" :key="zone" :label="zone" :value="zone" />
            </el-select>
            <span class="muted">IANA 时区；未登录顾客的时间按此显示，登录会员优先用会员时区。</span>
          </el-form-item>
          <el-form-item label="媒体 / 静态资源基址">
            <el-input v-model="form.media_public_base_url" placeholder="https://cdn.example.com 或留空跟随商城域名" />
            <span class="muted">数据库优先。换 CDN/对象存储只改这里；留空时跟随上面的商城对外域名。</span>
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
          <el-form-item label="营业执照 / 经营编号">
            <el-input v-model="form.business_license_no" placeholder="如：统一社会信用代码 91xxxxxxxxxxxxxxxx" />
          </el-form-item>
          <el-form-item label="客服电话">
            <el-input v-model="form.service_phone" />
          </el-form-item>
          <el-form-item label="客服邮箱">
            <el-input v-model="form.service_email" />
            <span class="field-hint">
              以上是全局值；「市场配置」里的同名项是市场级覆盖。商城按访客选择的地区
              （页脚「发货地区」选择器，请求带 X-Market）取市场值，市场没配才回退到这里。
            </span>
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>市场级覆盖与生效值</h3>
        <p class="muted">
          这些项在「系统设置」（全局）和「市场配置」（市场级）都能填。
          <b>市场配置里有值就以市场为准</b>；市场里没有这一行才回退全局。
          市场里填空串表示该项在本市场明确没有（例如香港没有 ICP 备案号），不再回退。
        </p>
        <el-table :data="effective" size="small" empty-text="暂无数据">
          <el-table-column prop="label" label="配置项" width="150" />
          <el-table-column label="全局值（系统设置）" min-width="190">
            <template #default="{ row }">{{ row.globalValue ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="市场值（市场配置）" min-width="190">
            <template #default="{ row }">
              <span v-if="row.marketValue === undefined" class="muted">未单独设置</span>
              <span v-else-if="row.marketValue === ''" class="muted">已留空（不回退全局）</span>
              <span v-else>{{ row.marketValue }}</span>
            </template>
          </el-table-column>
          <el-table-column label="当前生效" min-width="190">
            <template #default="{ row }">{{ row.effectiveValue ?? '—' }}</template>
          </el-table-column>
          <el-table-column label="来源" width="110">
            <template #default="{ row }">
              <el-tag size="small" :type="row.source === 'MARKET' ? 'warning' : 'info'">
                {{ row.source === 'MARKET' ? '市场配置' : '全局' }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <p class="muted">当前查看的市场：{{ effectiveMarket }}</p>
      </section>

      <section class="form-section">
        <h3>品牌与版权</h3>
        <p class="muted">版权声明默认显示。付费授权可关闭（建议作为买断/年费权益，见 docs/LICENSING_AND_BUILD.md）。关闭后页脚不再出现 “Powered by Full Mall”。</p>
        <div class="policy-grid">
          <el-form-item label="版权声明">
            <el-select v-model="form.branding_attribution" style="width: 100%">
              <el-option label="显示（社区版默认）" value="SHOW" />
              <el-option label="隐藏（付费授权）" value="HIDDEN" />
            </el-select>
          </el-form-item>
        </div>
      </section>

      <section class="form-section">
        <h3>访问域名</h3>
        <p class="muted">只填写 <code>域名[:端口]</code>，不要带 http:// 或路径。商城分享链接、SEO canonical、支付回调都会使用它；HTTPS 证书在「运维中心 → 证书」管理。</p>
        <div class="policy-grid">
          <el-form-item label="商城对外域名">
            <el-input v-model="form.site_domain" placeholder="company.binnarui.com:443" />
            <span v-if="siteUrlPreview" class="muted">对外地址预览：{{ siteUrlPreview }}</span>
          </el-form-item>
          <el-form-item label="对外协议">
            <el-select v-model="form.site_scheme" style="width: 100%">
              <el-option label="HTTPS" value="https" />
              <el-option label="HTTP" value="http" />
            </el-select>
          </el-form-item>
          <el-form-item label="说明">
            <span class="muted">示例：<code>company.binnarui.com:15443</code>、<code>192.168.1.15:41285</code>。协议按实际入口选择，不按端口猜。</span>
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
        <h3>素材上传</h3>
        <div class="policy-grid">
          <el-form-item label="管理端图片上限（MB）">
            <el-input-number v-model="adminImageMb" :min="0.1" :max="VALIDATION.assetUpload.admin.maxLimit / 1024 / 1024"
                             :step="0.5" :precision="1" style="width: 100%" />
            <p class="muted">商品图/规格图/素材库。默认 {{ (VALIDATION.assetUpload.admin.maxBytes / 1024 / 1024).toFixed(0) }}MB。</p>
          </el-form-item>
          <el-form-item label="管理端视频上限（MB）">
            <el-input-number v-model="adminVideoMb" :min="0.1" :max="VALIDATION.assetUpload.adminVideo.maxLimit / 1024 / 1024"
                             :step="1" :precision="1" style="width: 100%" />
            <p class="muted">主图视频/详情视频。默认 {{ (VALIDATION.assetUpload.adminVideo.maxBytes / 1024 / 1024).toFixed(0) }}MB。</p>
          </el-form-item>
          <el-form-item label="顾客端评价图片上限（MB）">
            <el-input-number v-model="memberImageMb" :min="0.1" :max="VALIDATION.assetUpload.member.maxLimit / 1024 / 1024"
                             :step="0.5" :precision="1" style="width: 100%" />
            <p class="muted">评价晒图。默认 {{ (VALIDATION.assetUpload.member.maxBytes / 1024 / 1024).toFixed(0) }}MB。</p>
          </el-form-item>
          <el-form-item label="顾客端评价视频上限（MB）">
            <el-input-number v-model="memberVideoMb" :min="0.1" :max="VALIDATION.assetUpload.memberVideo.maxLimit / 1024 / 1024"
                             :step="1" :precision="1" style="width: 100%" />
            <p class="muted">评价视频。默认 {{ (VALIDATION.assetUpload.memberVideo.maxBytes / 1024 / 1024).toFixed(0) }}MB。</p>
          </el-form-item>
        </div>
        <el-alert type="info" :closable="false" show-icon
                  :title="`单文件解析层天花板：${multipartCeilingMb}MB`"
                  description="超过天花板的文件在进入业务校验之前就会被拒绝；上限调大到接近天花板时，建议同时调高部署环境的 UPLOAD_MULTIPART_MAX_FILE_SIZE。" />
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
          <el-form-item label="已启用语言（逗号分隔）">
            <el-input v-model="form.enabled_locales" placeholder="en,ja" />
            <p class="muted">
              商品译文的语言页签、前台语言切换都以这里为准；保存后刷新商品编辑页即可看到新语言。
            </p>
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
              <el-option v-for="option in adapterOptions('storage')" :key="option.code"
                         :label="option.label + (option.available ? '' : '（' + option.note + '）')"
                         :value="option.code" :disabled="!option.available" />
            </el-select>
            <span class="muted">{{ adapterHint('storage', form.storage_provider) }}</span>
          </el-form-item>
          <!-- 短信不在这里配：按用途路由到具体供应商，见「第三方服务 → 通道路由」 -->
          <el-form-item label="短信">
            <span class="muted">短信按用途（登录验证码 / 订单通知 / 故障告警）在「第三方服务 → 通道路由」选择供应商；本页不再保存短信服务商。</span>
          </el-form-item>
          <el-form-item label="支付适配器">
            <el-select v-model="form.payment_provider" style="width: 100%">
              <el-option v-for="option in adapterOptions('payment')" :key="option.code"
                         :label="option.label + (option.available ? '' : '（' + option.note + '）')"
                         :value="option.code" :disabled="!option.available" />
            </el-select>
            <span class="muted">{{ adapterHint('payment', form.payment_provider) }}</span>
          </el-form-item>
          <el-form-item label="物流适配器">
            <el-select v-model="form.logistics_provider" style="width: 100%">
              <el-option v-for="option in adapterOptions('logistics')" :key="option.code"
                         :label="option.label + (option.available ? '' : '（' + option.note + '）')"
                         :value="option.code" :disabled="!option.available" />
            </el-select>
            <span class="muted">{{ adapterHint('logistics', form.logistics_provider) }}</span>
          </el-form-item>
        </div>
        <p class="muted">
          候选只列"现在真的能用"的实现：本地磁盘是内置实现（生产可直接用，不等同演示）；
          其它服务商需先在「第三方服务」配好凭据才会变成可选，未配置的置灰并写明原因。
        </p>

        <h3 style="margin-top: 16px">渠道自检</h3>
        <div class="policy-grid">
          <el-form-item label="测试渠道">
            <el-select v-model="testChannel" style="width: 100%">
              <el-option label="站内消息" value="IN_APP" />
              <el-option label="短信" value="SMS" />
              <el-option label="邮件" value="EMAIL" />
              <el-option label="微信" value="WECHAT" />
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

.field-hint {
  display: block;
  margin-top: 4px;
  color: #8a8f8a;
  font-size: 12px;
  line-height: 1.5;
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