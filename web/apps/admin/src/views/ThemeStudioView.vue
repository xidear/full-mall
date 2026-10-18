<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  clonePreset,
  themeNames,
  themePresets,
  toCssVars,
  type ThemeConfig,
  type ThemePresetName
} from '@fullmall/theme'
import { api } from '../api'

const config = reactive<ThemeConfig>(clonePreset('mall'))
const presetList = themeNames().map(name => themePresets[name])
const previewVars = computed(() => toCssVars(config))
const channel = ref<'PC' | 'MINI'>('PC')
const saving = ref(false)
const error = ref('')

onMounted(loadFromServer)
watch(channel, loadFromServer)

async function loadFromServer() {
  error.value = ''
  try {
    const response = await api.get<{ channel: string; market: string; config: ThemeConfig | null }>(
      '/api/v1/theme',
      { channel: channel.value, market: 'CN' }
    )
    if (response.config) {
      Object.assign(config, clonePreset(response.config.preset || 'mall'), response.config)
    } else {
      Object.assign(config, clonePreset('mall'))
    }
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '主题配置加载失败'
  }
}

function applyPreset(name: ThemePresetName) {
  Object.assign(config, clonePreset(name))
}

async function saveToServer() {
  saving.value = true
  error.value = ''
  try {
    await api.put('/admin/v1/theme', {
      channel: channel.value,
      market: 'CN',
      config
    })
    ElMessage.success(channel.value === 'PC' ? 'PC 主题已保存并生效' : '小程序主题已保存')
  } catch (cause) {
    error.value = cause instanceof Error ? cause.message : '保存失败'
  } finally {
    saving.value = false
  }
}

function saveLocal() {
  localStorage.setItem('fullmall.theme-config', JSON.stringify(config))
  ElMessage.success('主题配置已保存到本地')
}

async function copyConfig() {
  await navigator.clipboard.writeText(JSON.stringify(config, null, 2))
  ElMessage.success('主题配置已复制，可交给小程序构建流程')
}
</script>

<template>
  <div class="theme-studio">
    <div class="panel">
      <div class="panel-header">
        <div>
          <h2>主题工作室</h2>
          <p class="muted">所有调整都是选择器/滑块/开关，不需要手填 JSON 或逗号分隔字符串。</p>
        </div>
        <div>
          <el-button @click="saveLocal">保存到本地</el-button>
          <el-button @click="copyConfig">复制主题配置</el-button>
          <el-button type="primary" :loading="saving" @click="saveToServer">保存到服务器</el-button>
        </div>
      </div>

      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="mb-16" />
      <div class="preset-row">
        <span class="muted">应用端</span>
        <el-radio-group v-model="channel">
          <el-radio-button label="PC">PC 商城</el-radio-button>
          <el-radio-button label="MINI">微信小程序</el-radio-button>
        </el-radio-group>
        <span class="muted">预设模板</span>
        <el-button
          v-for="preset in presetList"
          :key="preset.preset"
          :type="config.preset === preset.preset ? 'primary' : 'default'"
          @click="applyPreset(preset.preset)"
        >
          {{ preset.label }}
        </el-button>
      </div>

      <div class="studio-grid">
        <section>
          <h3>品牌色</h3>
          <div class="color-grid">
            <label>主色<el-color-picker v-model="config.colors.brand" /></label>
            <label>深色<el-color-picker v-model="config.colors.brandDark" /></label>
            <label>强调色<el-color-picker v-model="config.colors.accent" /></label>
            <label>背景<el-color-picker v-model="config.colors.background" /></label>
            <label>卡片<el-color-picker v-model="config.colors.surface" /></label>
            <label>文字<el-color-picker v-model="config.colors.text" /></label>
            <label>次要文字<el-color-picker v-model="config.colors.muted" /></label>
            <label>分割线<el-color-picker v-model="config.colors.line" /></label>
          </div>

          <h3>形态</h3>
          <div class="policy-grid">
            <el-form-item label="圆角">
              <el-slider v-model="config.radius" :min="0" :max="32" show-input />
            </el-form-item>
            <el-form-item label="字号缩放">
              <el-slider v-model="config.fontScale" :min="0.85" :max="1.2" :step="0.01" show-input />
            </el-form-item>
            <el-form-item label="卡片风格">
              <el-select v-model="config.cardStyle" style="width: 100%">
                <el-option label="柔和" value="soft" />
                <el-option label="硬朗" value="sharp" />
                <el-option label="描边" value="outline" />
              </el-select>
            </el-form-item>
            <el-form-item label="商品布局">
              <el-select v-model="config.productLayout" style="width: 100%">
                <el-option label="网格" value="grid" />
                <el-option label="列表" value="list" />
              </el-select>
            </el-form-item>
          </div>

          <h3>首页</h3>
          <el-form label-position="top">
            <el-form-item label="显示 Banner">
              <el-switch v-model="config.showBanner" />
            </el-form-item>
            <el-form-item label="Banner 文案">
              <el-input v-model="config.bannerText" maxlength="40" show-word-limit />
            </el-form-item>
          </el-form>
        </section>

        <aside class="preview-column">
          <span class="muted">{{ channel === 'PC' ? 'PC 商城实时预览' : '微信小程序实时预览' }}</span>
          <div class="phone-preview" :style="previewVars">
            <div class="phone-status">9:41</div>
            <div class="phone-header">
              <strong>Full Mall</strong>
              <span>{{ config.label }}</span>
            </div>
            <div v-if="config.showBanner" class="phone-hero">
              <small>FULL MALL</small>
              <strong>{{ config.bannerText }}</strong>
            </div>
            <div class="phone-product" :class="config.cardStyle">
              <div class="phone-thumb">FM</div>
              <div>
                <strong>有机鲜牛奶</strong>
                <p>批次/效期 · 就近自提</p>
                <span class="phone-price">¥19.90</span>
              </div>
            </div>
            <div class="phone-product" :class="config.cardStyle">
              <div class="phone-thumb">FM</div>
              <div>
                <strong>手工陶瓷马克杯</strong>
                <p>常规库存 · 送货上门</p>
                <span class="phone-price">¥39.90</span>
              </div>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </div>
</template>

<style scoped>
.theme-studio .panel-header p {
  margin: 4px 0 0;
}

.preset-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-bottom: 22px;
}

.mb-16 {
  margin-bottom: 16px;
}

.studio-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 28px;
}

.studio-grid h3 {
  margin: 22px 0 12px;
  font-size: 15px;
}

.color-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.color-grid label {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border: 1px solid #ece9e2;
  border-radius: 10px;
  color: #6b716c;
  font-size: 12px;
}

.preview-column {
  align-self: start;
}

.phone-preview {
  margin-top: 10px;
  padding: 18px;
  border: 1px solid var(--line);
  border-radius: 28px;
  background: var(--bg);
  color: var(--text);
  box-shadow: 0 20px 45px rgba(0, 0, 0, 0.12);
}

.phone-status {
  margin-bottom: 10px;
  color: var(--muted);
  font-size: 10px;
  text-align: center;
}

.phone-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  margin-bottom: 14px;
}

.phone-header span {
  color: var(--muted);
  font-size: 10px;
}

.phone-hero {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 18px;
  border-radius: var(--radius);
  background: var(--surface-muted);
}

.phone-hero small {
  color: var(--accent);
  font-size: 9px;
  letter-spacing: 0.12em;
}

.phone-hero strong {
  font-size: 18px;
  line-height: 1.2;
}

.phone-product {
  display: flex;
  gap: 10px;
  margin-top: 12px;
  padding: 10px;
  border: 1px solid var(--line);
  background: var(--surface);
}

.phone-product.soft {
  border-radius: var(--radius);
}

.phone-product.sharp {
  border-radius: 0;
}

.phone-product.outline {
  border-radius: 0;
  border-color: var(--brand);
}

.phone-thumb {
  display: grid;
  width: 58px;
  height: 58px;
  place-items: center;
  border-radius: calc(var(--radius) / 2);
  background: var(--surface-muted);
  color: var(--muted);
  font-size: 14px;
  font-weight: 800;
}

.phone-product strong {
  font-size: 12px;
}

.phone-product p {
  margin: 3px 0;
  color: var(--muted);
  font-size: 10px;
}

.phone-price {
  color: var(--brand);
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 900px) {
  .studio-grid {
    grid-template-columns: 1fr;
  }
}
</style>