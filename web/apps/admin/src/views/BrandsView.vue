<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'

interface Brand {
  id: string
  code: string
  name: string
  sortOrder: number
  productCount: number
}

const brands = ref<Brand[]>([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const form = ref({ code: '', name: '', sortOrder: 0 })
const editingId = ref('')

async function load() {
  loading.value = true
  try {
    brands.value = await api.get<Brand[]>('/admin/v1/brands')
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '品牌加载失败')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editingId.value = ''
  form.value = { code: '', name: '', sortOrder: 0 }
  dialog.value = true
}

function openEdit(brand: Brand) {
  editingId.value = brand.id
  form.value = { code: brand.code, name: brand.name, sortOrder: brand.sortOrder }
  dialog.value = true
}

async function save() {
  if (saving.value) return
  if (!form.value.name.trim()) {
    ElMessage.error('请填写品牌名称')
    return
  }
  if (!editingId.value && !form.value.code.trim()) {
    ElMessage.error('请填写品牌编码')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await api.put(`/admin/v1/brands/${editingId.value}`,
        { name: form.value.name, sortOrder: form.value.sortOrder })
      ElMessage.success('已保存')
    } else {
      await api.post('/admin/v1/brands', {
        code: form.value.code.trim().toUpperCase(),
        name: form.value.name.trim(),
        sortOrder: form.value.sortOrder
      })
      ElMessage.success('已完成新建')
    }
    dialog.value = false
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function remove(brand: Brand) {
  try {
    await ElMessageBox.confirm(
      brand.productCount > 0
        ? `该品牌已被 ${brand.productCount} 个商品引用，删除会被拒绝。仍要尝试？`
        : `确认删除品牌「${brand.name}」？`,
      '删除确认', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.delete(`/admin/v1/brands/${brand.id}`)
    ElMessage.success('已删除')
    await load()
  } catch (cause) {
    ElMessage.error(cause instanceof Error ? cause.message : '删除失败')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h1>品牌管理</h1>
    <el-alert type="info" :closable="false" show-icon class="mb-16"
              title="品牌用于商品归属与促销/券的「指定品牌」范围"
              description="已被商品引用的品牌不能删除；删除前请先把商品改到其它品牌。" />

    <div class="header-actions mb-16">
      <el-button type="primary" @click="openCreate">新建品牌</el-button>
      <el-button plain :loading="loading" @click="load">刷新</el-button>
    </div>

    <el-table :data="brands" v-loading="loading" empty-text="暂无品牌">
      <el-table-column prop="code" label="编码" width="180" />
      <el-table-column prop="name" label="名称" min-width="200" />
      <el-table-column prop="sortOrder" label="排序" width="90" />
      <el-table-column label="关联商品" width="110">
        <template #default="{ row }">{{ row.productCount }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">改名</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="editingId ? '编辑品牌' : '新建品牌'" width="420px">
      <el-form label-width="90px">
        <el-form-item label="编码">
          <el-input v-model="form.code" :disabled="!!editingId" placeholder="例如 BRAND-A" />
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="例如 示例品牌" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sortOrder" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.header-actions { display: flex; gap: 12px; }
.mb-16 { margin-bottom: 16px; }
</style>
