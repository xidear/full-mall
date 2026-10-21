<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import { helpFor } from '../help/catalog'

const route = useRoute()
const entry = computed(() => helpFor(route.path))
const open = ref(false)
</script>

<template>
  <div class="help-wrap">
    <el-button text class="help-btn" @click="open = true">帮助</el-button>

    <el-drawer v-model="open" :title="entry ? entry.title + ' · 使用帮助' : '使用帮助'" size="560px">
      <template v-if="entry">
        <p class="desc">{{ entry.description }}</p>

        <h4>操作步骤</h4>
        <ol class="steps">
          <li v-for="(step, index) in entry.steps" :key="index">{{ step }}</li>
        </ol>

        <h4>运营建议</h4>
        <ul class="tips">
          <li v-for="(tip, index) in entry.tips" :key="index">{{ tip }}</li>
        </ul>

        <h4>常见错误</h4>
        <ul class="pitfalls">
          <li v-for="(item, index) in entry.pitfalls" :key="index">{{ item }}</li>
        </ul>

        <template v-if="entry.related?.length">
          <h4>相关页面</h4>
          <p class="related">
            <router-link v-for="link in entry.related" :key="link" :to="link" class="related-link">
              {{ link }}
            </router-link>
          </p>
        </template>
      </template>

      <el-alert v-else type="warning" :closable="false" show-icon
                title="该页面还没有帮助内容"
                description="这是覆盖率检查应当拦住的情况，请补充 src/help/catalog.ts。" />
    </el-drawer>
  </div>
</template>

<style scoped>
.help-wrap { display: inline-block; }
.help-btn { font-size: 13px; }
.desc { color: var(--el-text-color-regular); line-height: 1.7; }
h4 { margin: 18px 0 8px; font-size: 14px; }
.steps, .tips, .pitfalls { margin: 0; padding-left: 20px; line-height: 1.8; font-size: 13px; }
.tips { color: var(--el-color-primary); }
.pitfalls { color: var(--el-color-danger); }
.related-link { margin-right: 12px; }
</style>
