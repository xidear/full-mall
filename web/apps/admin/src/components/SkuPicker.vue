<script setup lang="ts">
import { computed } from 'vue'
import { formatMoney } from '@fullmall/api-client'

interface Sku { id: string; skuCode: string; price?: { amount: number; currency?: string } }
interface Product { id: string; title: string; skus?: Sku[] }

const props = withDefaults(defineProps<{
  modelValue: string
  product: Product | null
  width?: string
}>(), { width: '320px' })

const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const skus = computed(() => props.product?.skus ?? [])

function price(sku: Sku): string {
  if (!sku.price?.currency) return ''
  return formatMoney({ amount: sku.price.amount, currency: sku.price.currency })
}
</script>

<template>
  <el-select :model-value="props.modelValue" clearable filterable
             :placeholder="product ? '选择规格（SKU）' : '先选择商品'"
             :disabled="!product" :style="{ width }"
             @update:model-value="(value: string) => emit('update:modelValue', value)">
    <el-option v-for="sku in skus" :key="sku.id"
               :label="`${sku.skuCode} ${price(sku)}`" :value="sku.id" />
  </el-select>
</template>
