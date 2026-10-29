<script setup lang="ts">
/**
 * 国别地址表单（ADR-0013 §4 / ADR-0014）。
 *
 * 形状由后端下发的 address_format 决定：
 *   required=ACSZ 且 fmt 含 %S %C %D → 渲染对应字段；没有 %C 的国家（如新加坡）就不渲染城市框。
 * 一级行政区只有在 hasAdminList=true 时才做下拉，否则是文本输入——
 * 多数国家的市/区不是封闭枚举（德法日巴的市镇是万级且随时改名）。
 */
import { computed, onMounted, ref, watch } from 'vue'
import { useLocale } from '~/composables/useLocale'
import type { AddressModel } from '~/utils/address'

interface AddressFormat {
  countryCode: string
  required: string
  fmt: string
  zip: string | null
  zipex: string | null
  stateNameType: string
  hasAdminList: boolean
  adminOptional: boolean
  hasLocality: boolean
  hasDependentLocality: boolean
}

interface RegionOption { code: string; parentCode: string | null; level: string; depth: number; name: string }

const props = defineProps<{
  modelValue: AddressModel
  /** 初始国家（一般取市场所属国家），留空则不预选 */
  defaultCountry?: string
}>()
const emit = defineEmits<{ 'update:modelValue': [AddressModel] }>()

const { t, locale } = useLocale()
const { market } = useSite()
const config = useRuntimeConfig()
const apiBase = import.meta.server ? config.apiInternalBase : config.public.apiBase

const countries = ref<RegionOption[]>([])
const format = ref<AddressFormat | null>(null)
const adminAreas = ref<RegionOption[]>([])
const localities = ref<RegionOption[]>([])
const districts = ref<RegionOption[]>([])
const loadingCountries = ref(false)
const loadingFormat = ref(false)
/** 该国没配地址格式（未抓快照）：明确提示不支持，不套中国模板 */
const unsupported = ref(false)
/**
 * 该市场是否开通了"配送到这个国家"。与后端下单门禁同源（/capabilities），
 * 这样前台不会出现"能填完表单、点提交才报错"的体验。
 */
const undeliverable = ref(false)

const model = computed(() => props.modelValue)

function patch(partial: Partial<AddressModel>) {
  emit('update:modelValue', { ...props.modelValue, ...partial })
}

async function fetchJson<T>(path: string): Promise<T | null> {
  try {
    return await $fetch<T>(`${apiBase}${path}`)
  } catch (cause: unknown) {
    const status = (cause as { statusCode?: number; status?: number })?.statusCode
      ?? (cause as { status?: number })?.status
    if (status === 404) return null
    throw cause
  }
}

async function loadCountries() {
  loadingCountries.value = true
  try {
    countries.value = await fetchJson<RegionOption[]>(`/api/v1/regions/countries?locale=${locale.value}`) ?? []
    if (!props.modelValue.countryCode && props.defaultCountry) {
      patch({ countryCode: props.defaultCountry })
    }
  } finally {
    loadingCountries.value = false
  }
}

async function loadFormat(countryCode: string) {
  if (!countryCode) {
    format.value = null
    adminAreas.value = []
    localities.value = []
    unsupported.value = false
    return
  }
  loadingFormat.value = true
  unsupported.value = false
  undeliverable.value = false
  try {
    const capabilities = await fetchJson<{ deliverable: boolean }>(
      `/api/v1/markets/${market.value}/capabilities?destination=${countryCode}`)
    undeliverable.value = capabilities ? !capabilities.deliverable : false
    const loaded = await fetchJson<AddressFormat>(`/api/v1/regions/formats?country=${countryCode}`)
    format.value = loaded
    if (!loaded) {
      // 没有格式 = 未开通配送：清掉区划，让上层能看到 unsupported
      unsupported.value = true
      adminAreas.value = []
      localities.value = []
      return
    }
    if (loaded.hasAdminList) {
      // 用入参 countryCode，不要用 props.modelValue：patch 之后父组件的 props 要下一轮才更新，
      // 这里读 props 会拿到上一个国家的码（甚至空串），导致下拉永远是空的。
      adminAreas.value = await fetchJson<RegionOption[]>(
        `/api/v1/regions?country=${countryCode}&locale=${locale.value}`) ?? []
    } else {
      adminAreas.value = []
    }
    localities.value = []
  } finally {
    loadingFormat.value = false
  }
}

async function onCountryChange(code: string) {
  patch({ countryCode: code, regionCode: '', province: '', city: '', district: '' })
  localities.value = []
  districts.value = []
  await loadFormat(code)
}

async function onAdminChange(code: string) {
  const picked = adminAreas.value.find(option => option.code === code)
  const country = props.modelValue.countryCode
  patch({ regionCode: code, province: picked?.name ?? '', city: '', district: '' })
  localities.value = []
  districts.value = []
  if (!code) return
  // 该节点若还有下级（中国：省 → 市），继续取；没有下级时返回空数组，城市改由文本输入
  if (format.value?.hasLocality) {
    localities.value = await fetchJson<RegionOption[]>(
      `/api/v1/regions?country=${country}&parent=${code}&locale=${locale.value}`) ?? []
  }
}

function onLocalityChange(code: string) {
  const picked = localities.value.find(option => option.code === code)
  const country = props.modelValue.countryCode
  patch({ city: picked?.name ?? '', district: '' })
  districts.value = []
  if (!code || !format.value?.hasDependentLocality) {
    return
  }
  // 区县级（中国是 省→市→区）：抓到了就级联，抓不到就退回文本输入，不假装没有这一级
  void (async () => {
    districts.value = await fetchJson<RegionOption[]>(
      `/api/v1/regions?country=${country}&parent=${code}&locale=${locale.value}`) ?? []
  })()
}

function onDistrictChange(code: string) {
  const picked = districts.value.find(option => option.code === code)
  patch({ district: picked?.name ?? '' })
}

/** 邮编按该国正则校验；renders 前不拦，提交时由后端权威校验，这里只做即时提示。 */
const postalInvalid = computed(() => {
  const rule = format.value?.zip
  const value = props.modelValue.postalCode
  if (!rule || !value) return false
  try {
    return !new RegExp(`^(?:${rule})$`).test(value.trim())
  } catch {
    return false
  }
})

/** zipex 是逗号分隔的多个示例（如 "154-0023,350-1106"），占位符只取第一个。 */
const postalExample = computed(() => (format.value?.zipex ?? '').split(',')[0]?.trim() ?? '')

const stateLabel = computed(() => {
  const type = format.value?.stateNameType
  return type ? t(`address.stateType.${type}`) : t('address.state')
})

onMounted(async () => {
  await loadCountries()
  if (props.modelValue.countryCode) {
    await loadFormat(props.modelValue.countryCode)
  }
})

watch(() => props.defaultCountry, async (next) => {
  if (next && !props.modelValue.countryCode) {
    patch({ countryCode: next })
    await loadFormat(next)
  }
})
</script>

<template>
  <div class="address-form">
    <div class="field">
      <label for="addr-country">{{ t('address.country') }}</label>
      <select
        id="addr-country"
        :value="model.countryCode"
        :disabled="loadingCountries"
        @change="onCountryChange(($event.target as HTMLSelectElement).value)"
      >
        <option value="">{{ loadingCountries ? '…' : t('address.select') }}</option>
        <option v-for="country in countries" :key="country.code" :value="country.code">
          {{ country.name }}
        </option>
      </select>
    </div>

    <p v-if="unsupported" class="field-hint error-text">{{ t('address.formatMissing') }}</p>
    <p v-if="undeliverable" class="field-hint error-text">{{ t('address.notDeliverable') }}</p>

    <template v-if="format && !unsupported">
      <!-- 一级行政区：有封闭枚举才下拉，否则文本输入 -->
      <div v-if="format.required.includes('S') || !format.adminOptional" class="field">
        <label for="addr-state">{{ stateLabel }}</label>
        <select
          v-if="format.hasAdminList && adminAreas.length"
          id="addr-state"
          :value="model.regionCode"
          :disabled="loadingFormat"
          @change="onAdminChange(($event.target as HTMLSelectElement).value)"
        >
          <option value="">{{ t('address.select') }}</option>
          <option v-for="area in adminAreas" :key="area.code" :value="area.code">{{ area.name }}</option>
        </select>
        <input
          v-else
          id="addr-state"
          :value="model.province"
          type="text"
          @input="patch({ province: ($event.target as HTMLInputElement).value })"
        >
      </div>

      <!-- 城市：只有该国地址模板里真的有 %C 才渲染 -->
      <div v-if="format.hasLocality" class="field">
        <label for="addr-city">{{ t('address.city') }}</label>
        <select
          v-if="localities.length"
          id="addr-city"
          :value="localities.find(x => x.name === model.city)?.code ?? ''"
          @change="onLocalityChange(($event.target as HTMLSelectElement).value)"
        >
          <option value="">{{ t('address.select') }}</option>
          <option v-for="option in localities" :key="option.code" :value="option.code">{{ option.name }}</option>
        </select>
        <input
          v-else
          id="addr-city"
          :value="model.city"
          type="text"
          :placeholder="t('address.city')"
          @input="patch({ city: ($event.target as HTMLInputElement).value })"
        >
      </div>

      <div v-if="format.hasDependentLocality" class="field">
        <label for="addr-district">{{ t('address.district') }}</label>
        <select
          v-if="districts.length"
          id="addr-district"
          :value="districts.find(x => x.name === model.district)?.code ?? ''"
          @change="onDistrictChange(($event.target as HTMLSelectElement).value)"
        >
          <option value="">{{ t('address.select') }}</option>
          <option v-for="option in districts" :key="option.code" :value="option.code">{{ option.name }}</option>
        </select>
        <input
          v-else
          id="addr-district"
          :value="model.district"
          type="text"
          :placeholder="t('address.district')"
          @input="patch({ district: ($event.target as HTMLInputElement).value })"
        >
      </div>

      <div v-if="format.required.includes('Z') || !!format.zip" class="field">
        <label for="addr-postal">{{ t('address.postalCode') }}</label>
        <input
          id="addr-postal"
          :value="model.postalCode"
          type="text"
          :placeholder="postalExample"
          autocomplete="postal-code"
          @input="patch({ postalCode: ($event.target as HTMLInputElement).value })"
        >
        <p v-if="postalInvalid" class="field-hint error-text">{{ t('address.postalInvalid') }}</p>
      </div>

      <div class="field">
        <label for="addr-detail">{{ t('checkout.address') }}</label>
        <input
          id="addr-detail"
          :value="model.detail"
          type="text"
          :placeholder="t('checkout.addressPlaceholder')"
          autocomplete="street-address"
          @input="patch({ detail: ($event.target as HTMLInputElement).value })"
        >
      </div>

      <p v-if="!format.hasAdminList && !adminAreas.length && format.required.includes('S')" class="field-hint">
        {{ t('address.noRegionData') }}
      </p>
    </template>
  </div>
</template>
