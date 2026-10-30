<script setup lang="ts">
/**
 * 列表内联状态开关。
 *
 * <p>为什么要有：启用/停用是一眼就能判断的动作，让运营打开弹窗、改、保存，
 * 是把简单操作复杂化。列表里点一下即可切换。
 *
 * <p>交互约定：
 * - 停用影响面较大 → 二次确认；启用直接生效；
 * - 切换期间禁用，防连点；
 * - **接口成功才翻转显示**（用 el-switch 的 before-change：返回 false 就保持原状），
 *   失败保持旧值并报错，不假装成功。
 */
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = withDefaults(defineProps<{
  active: boolean
  activeText?: string
  inactiveText?: string
  disabled?: boolean
  confirmOnDeactivate?: boolean
  confirmText?: string
  onChange: (next: boolean) => Promise<void>
}>(), {
  activeText: '启用',
  inactiveText: '停用',
  disabled: false,
  confirmOnDeactivate: true,
  confirmText: '停用后将立即对该对象生效，确定继续吗？'
})

const pending = ref(false)

async function beforeChange(): Promise<boolean> {
  if (pending.value || props.disabled) {
    return false
  }
  const next = !props.active
  if (!next && props.confirmOnDeactivate) {
    try {
      await ElMessageBox.confirm(props.confirmText, '确认停用', {
        confirmButtonText: '确认停用', cancelButtonText: '取消', type: 'warning'
      })
    } catch {
      return false
    }
  }
  pending.value = true
  try {
    await props.onChange(next)
    ElMessage.success(next ? '已' + props.activeText : '已' + props.inactiveText)
    return true
  } catch (cause) {
    ElMessage.error('切换失败：' + (cause instanceof Error ? cause.message : String(cause)))
    return false
  } finally {
    pending.value = false
  }
}
</script>

<template>
  <el-switch :model-value="active" :loading="pending" :disabled="disabled || pending"
             :before-change="beforeChange"
             :active-text="activeText" :inactive-text="inactiveText"
             inline-prompt style="--el-switch-on-color: #2f9e44; --el-switch-off-color: #adb5bd" />
</template>
