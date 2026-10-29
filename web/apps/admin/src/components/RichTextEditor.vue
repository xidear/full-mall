<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import AssetPicker from './AssetPicker.vue'

/**
 * 富文本详情编辑器（TipTap，MIT）。
 *
 * <p>只允许"受限 HTML"：段落/标题/列表/加粗斜体/链接/图片/视频/图注。
 * 服务端保存时还会用 jsoup 白名单再清洗一遍（前端可绕过，服务端才是权威）。
 */
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const imageUrl = ref('')
const videoUrl = ref('')

const editor = new Editor({
  content: props.modelValue || '',
  extensions: [
    StarterKit,
    Image.configure({ inline: false, allowBase64: false })
  ],
  onUpdate: ({ editor: instance }) => emit('update:modelValue', instance.getHTML())
})

watch(() => props.modelValue, (value) => {
  if (value !== editor.getHTML()) {
    editor.commands.setContent(value || '', false)
  }
})

onBeforeUnmount(() => editor.destroy())

function insertImage() {
  const url = imageUrl.value.trim()
  if (!url) return
  editor.chain().focus().setImage({ src: url }).run()
  imageUrl.value = ''
}

function insertVideo() {
  const url = videoUrl.value.trim()
  if (!url) return
  // TipTap 没有官方 video 扩展：插入原生 <video>，服务端白名单允许 video 标签
  editor.chain().focus().insertContent(
    '<figure><video src="' + url + '" controls playsinline></video><figcaption>视频</figcaption></figure>'
  ).run()
  videoUrl.value = ''
}
</script>

<template>
  <div class="rich-editor">
    <div class="toolbar">
      <el-button size="small" @click="editor.chain().focus().toggleBold().run()">粗体</el-button>
      <el-button size="small" @click="editor.chain().focus().toggleItalic().run()">斜体</el-button>
      <el-button size="small" @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">标题</el-button>
      <el-button size="small" @click="editor.chain().focus().toggleBulletList().run()">无序列表</el-button>
      <el-button size="small" @click="editor.chain().focus().toggleOrderedList().run()">有序列表</el-button>
      <el-button size="small" @click="editor.chain().focus().toggleBlockquote().run()">引用</el-button>
      <span class="divider" />
      <AssetPicker v-model="imageUrl" value-type="url" label="插入图片" />
      <el-input v-model="videoUrl" size="small" placeholder="视频地址（/assets/... 或 http(s)://）"
                style="width: 220px" />
      <el-button size="small" @click="insertVideo">插入视频</el-button>
    </div>
    <EditorContent :editor="editor" class="content" />
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid #dedbd3;
  border-radius: 10px;
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px;
  background: #faf9f6;
  border-bottom: 1px solid #ece9e2;
}

.divider {
  width: 1px;
  height: 20px;
  background: #dedbd3;
}

.content {
  padding: 12px 14px;
  min-height: 220px;
}

.content :deep(.ProseMirror) {
  outline: none;
  min-height: 200px;
}

.content :deep(img),
.content :deep(video) {
  max-width: 100%;
  border-radius: 6px;
}

.content :deep(figcaption) {
  font-size: 12px;
  color: #5f666d;
  text-align: center;
}
</style>
