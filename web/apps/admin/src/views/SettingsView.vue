<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
  logistics_provider: 'mock'
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