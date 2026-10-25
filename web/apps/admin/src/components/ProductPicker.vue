<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { api } from '../api'

interface Sku { id: string; skuCode: string; price?: { amount: number; currency?: string } }
interface Product { id: string; title: string; skus?: Sku[] }

const props = withDefaults(defineProps<{
  modelValue: string
  placeholder?: string
  width?: string
}>(), { placeholder: '搜索并选择商品', width: '320px' })

const emit = defineEmits<{
  'update:modelValue': [value: string]
  'select': [product: Product | null]
}>()

const options = ref<Product[]>([])
const loading = ref(false)
const keyword = ref('')
let timer: ReturnType<typeof setTimeout> | null = null

async function search(text: string) {
  loading.value = true
  try {
    const page = await api.get<{ items: Product[] }>('/admin/v1/products', {
      page: 0, size: 20, keyword: text || undefined
    })
    options.value = page.items ?? []
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '商品搜索失败')
  } finally {
    loading.value = false
  }
}

function onSearch(text: string) {
  keyword.value = text
  if (timer) clearTimeout(timer)
  timer = setTimeout(() => void search(text), 250)
}

function onChange(id: string) {
  emit('update:modelValue', id)
  emit('select', options.value.find(product => product.id === id) ?? null)
}

watch(() => props.modelValue, id => {
  if (id && !options.value.some(product => product.id === id)) {
    void search('').then(() => {
      emit('select', options.value.find(product => product.id === id) ?? null)
    })
  }
})

onMounted(() => void search(''))
</script>

<template>
  <el-select :model-value="props.modelValue" filterable remote reserve-keyword clearable
             :remote-method="onSearch" :loading="loading" :placeholder="placeholder"
             :style="{ width }" @update:model-value="onChange">
    <el-option v-for="product in options" :key="product.id"
               :label="`${product.title}（${product.skus?.length ?? 0} 规格）`" :value="product.id" />
  </el-select>
</template>
