<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'
import { Editor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import { TableKit } from '@tiptap/extension-table'
import Placeholder from '@tiptap/extension-placeholder'
import CharacterCount from '@tiptap/extension-character-count'
import TextAlign from '@tiptap/extension-text-align'
import { ElMessage } from 'element-plus'
import AssetPicker from './AssetPicker.vue'
import { apiErrorMessage } from '../composables/useAsyncState'
import { uploadMedia } from '../upload'

/**
 * 富文本详情编辑器（TipTap 3.x，MIT）。
 *
 * <p>只产出"受限 HTML"：段落/标题/列表/引用/表格/加粗斜体下划线/链接/图片/视频/图注。
 * 服务端 RichTextSanitizer + RichTextMediaGuard 才是权威（前端可被绕过）。
 *
 * <p>三条刻意为之的约定，改之前先读完：
 * <ol>
 *   <li><b>所有上传走 uploadMedia()</b>：它内部打 /admin/v1/media，落对象存储并登记进 asset 表。
 *       编辑器不自带上传端、不写 base64、不直插外站图——否则素材脱离素材库，
 *       保存时被服务端 RICH_TEXT_MEDIA_NOT_IN_LIBRARY 拒绝（这正是不要 UEditor 的原因）。</li>
 *   <li><b>图片带 data-asset-id</b>：服务端按 id 校验归属，而不是拿 URL 字符串猜；
 *       素材换地址/CDN 迁移时也能定位到具体素材。</li>
 *   <li><b>Link / Underline 不单独注册</b>：TipTap 3 的 StarterKit 已内置这两个扩展，
 *       再注册一次会抛 "Duplicate extension names found"。</li>
 * </ol>
 */
const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const revision = ref(0)
const uploading = ref(0)
const imageUrl = ref('')
const videoUrl = ref('')

/** 素材库图片带上 assetId：服务端用它校验"这张图确实在素材库里"。 */
const AssetImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      assetId: {
        default: null,
        parseHTML: element => element.getAttribute('data-asset-id'),
        renderHTML: attributes => (attributes.assetId ? { 'data-asset-id': attributes.assetId } : {})
      }
    }
  }
}).configure({ inline: false, allowBase64: false })

const editor = new Editor({
  content: props.modelValue || '',
  extensions: [
    StarterKit.configure({
      link: { openOnClick: false, autolink: false, HTMLAttributes: { rel: 'noopener noreferrer', target: '_blank' } }
    }),
    AssetImage,
    TableKit.configure({ table: { resizable: false } }),
    Placeholder.configure({ placeholder: '写商品详情：文字、图片、视频、表格都可以混排' }),
    CharacterCount,
    // 对齐只作用于段落与标题：服务端只保留 text-align 这一条样式，其它 style 一律丢弃
    TextAlign.configure({ types: ['heading', 'paragraph'], alignments: ['left', 'center', 'right'] })
  ],
  editorProps: {
    attributes: { class: 'rich-content' },
    handleDrop(view, event, _slice, moved) {
      if (moved) return false
      const files = Array.from(event.dataTransfer?.files ?? [])
      if (!files.length) return false
      event.preventDefault()
      const at = view.posAtCoords({ left: event.clientX, top: event.clientY })?.pos
      files.forEach(file => void uploadAndInsert(file, at))
      return true
    },
    handlePaste(view, event) {
      const files = Array.from(event.clipboardData?.files ?? [])
      if (files.length) {
        event.preventDefault()
        files.forEach(file => void uploadAndInsert(file))
        return true
      }
      const html = event.clipboardData?.getData('text/html') ?? ''
      const stripped = stripExternalImages(html)
      if (stripped !== html) {
        event.preventDefault()
        editor.commands.insertContent(stripped)
        ElMessage.warning('已略过外站图片：外链图片必须先上传进素材库，否则保存会被服务端拒绝')
        return true
      }
      return false
    }
  },
  onUpdate: ({ editor: instance }) => emit('update:modelValue', instance.getHTML()),
  // 工具栏高亮与字数不在 DOM 事件里更新，靠这个计数器触发重渲染
  onTransaction: () => { revision.value++ },
  onSelectionUpdate: () => { revision.value++ }
})

watch(() => props.modelValue, value => {
  if (value !== editor.getHTML()) {
    // TipTap v3 把第二个参数从 emitUpdate: boolean 换成了 options 对象（v2 写法在 v3 会类型报错）
    editor.commands.setContent(value || '', { emitUpdate: false })
  }
})

onBeforeUnmount(() => editor.destroy())

function characterCount(): number {
  const storage = editor.storage.characterCount as { characters?: () => number } | undefined
  return storage?.characters?.() ?? 0
}

/**
 * 外站图片一律拦下：从网页粘贴时浏览器会给 <img src="https://...">，
 * 这种图保存必被服务端拒绝；与其让运营在点保存那一步失败，不如现在就告知并丢弃图片。
 */
function stripExternalImages(html: string): string {
  if (!html || !/<img/i.test(html)) return html
  const doc = new DOMParser().parseFromString(html, 'text/html')
  let removed = 0
  doc.querySelectorAll('img[src]').forEach(image => {
    const src = image.getAttribute('src') ?? ''
    if (!src.startsWith('/') || src.startsWith('data:')) {
      image.remove()
      removed++
    }
  })
  return removed > 0 ? doc.body.innerHTML : html
}

/** 拖拽/粘贴/选择文件都走这里：上传成功才插入；失败明确报错，不留半截内容。 */
async function uploadAndInsert(file: File, at?: number) {
  uploading.value++
  try {
    const media = await uploadMedia(file, 'PRODUCT')
    if (media.kind === 'VIDEO') {
      insertAt('<figure><video src="' + media.url + '" controls playsinline'
        + (media.assetId ? ' data-asset-id="' + media.assetId + '"' : '')
        + '></video><figcaption>视频</figcaption></figure>', at)
    } else {
      insertAt({
        type: 'image',
        attrs: { src: media.url, assetId: media.assetId, alt: file.name }
      }, at)
    }
  } catch (cause) {
    ElMessage.error('上传失败，未插入内容：' + apiErrorMessage(cause, '未知原因'))
  } finally {
    uploading.value--
  }
}

function insertAt(content: unknown, at?: number) {
  const chain = editor.chain().focus()
  if (at === undefined) {
    chain.insertContent(content as never).run()
  } else {
    chain.insertContentAt(at, content as never).run()
  }
}

function insertFromLibrary() {
  const url = imageUrl.value.trim()
  if (!url) {
    ElMessage.warning('请先在素材库里选一张图片')
    return
  }
  insertAt({ type: 'image', attrs: { src: url } })
  imageUrl.value = ''
}

function insertVideoByUrl() {
  const url = videoUrl.value.trim()
  if (!url) {
    ElMessage.warning('请填写素材库里的视频地址')
    return
  }
  insertAt('<figure><video src="' + url + '" controls playsinline></video><figcaption>视频</figcaption></figure>')
  videoUrl.value = ''
}

function pickFile(refName: 'imageFile' | 'videoFile') {
  const el = document.querySelector(('[data-upload=' + refName + ']')) as HTMLInputElement | null
  if (!el) {
    ElMessage.error('上传控件未渲染：请刷新页面后重试')
    return
  }
  el.click()
}

function onFilePicked(event: Event, input: HTMLInputElement) {
  const file = input.files?.[0]
  // 清空 value：同一个文件连续选两次也要触发 change
  input.value = ''
  if (file) void uploadAndInsert(file)
}

function askLink() {
  const previous = editor.getAttributes('link').href as string | undefined
  const href = window.prompt('链接地址（站内 / 开头或 http(s)://）', previous ?? '')
  if (href === null) return
  if (!href.trim()) {
    editor.chain().focus().extendMarkRange('link').unsetLink().run()
    return
  }
  editor.chain().focus().extendMarkRange('link').setLink({ href: href.trim() }).run()
}
</script>

<template>
  <div class="rich-editor">
    <div class="toolbar">
      <el-button size="small" :type="editor.isActive('bold') ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleBold().run()">粗体</el-button>
      <el-button size="small" :type="editor.isActive('italic') ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleItalic().run()">斜体</el-button>
      <el-button size="small" :type="editor.isActive('underline') ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleUnderline().run()">下划线</el-button>
      <el-button size="small" :type="editor.isActive('heading', { level: 2 }) ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleHeading({ level: 2 }).run()">标题</el-button>
      <el-button size="small" :type="editor.isActive('heading', { level: 3 }) ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleHeading({ level: 3 }).run()">小标题</el-button>
      <el-button size="small" :type="editor.isActive('bulletList') ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleBulletList().run()">无序列表</el-button>
      <el-button size="small" :type="editor.isActive('orderedList') ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleOrderedList().run()">有序列表</el-button>
      <el-button size="small" :type="editor.isActive('blockquote') ? 'primary' : 'default'"
                 @click="editor.chain().focus().toggleBlockquote().run()">引用</el-button>
      <el-button size="small" :type="editor.isActive('link') ? 'primary' : 'default'"
                 @click="askLink">链接</el-button>
      <el-button size="small" @click="editor.chain().focus().insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run()">表格</el-button>
      <el-button v-if="editor.isActive('table')" size="small"
                 @click="editor.chain().focus().deleteTable().run()">删除表格</el-button>
    </div>

    <div class="toolbar">
      <el-button size="small" :loading="uploading > 0" @click="pickFile('imageFile')">上传图片</el-button>
      <input data-upload="imageFile" type="file" accept="image/*" hidden
             @change="(event) => onFilePicked(event, event.target as HTMLInputElement)">
      <AssetPicker v-model="imageUrl" value-type="url" label="素材库选图" />
      <el-button size="small" @click="insertFromLibrary">插入</el-button>
      <span class="divider" />
      <el-button size="small" :loading="uploading > 0" @click="pickFile('videoFile')">上传视频</el-button>
      <input data-upload="videoFile" type="file" accept="video/*" hidden
             @change="(event) => onFilePicked(event, event.target as HTMLInputElement)">
      <el-input v-model="videoUrl" size="small" placeholder="素材库视频地址 /assets/…"
                style="width: 200px" />
      <el-button size="small" @click="insertVideoByUrl">插入视频</el-button>
      <span class="divider" />
      <el-button size="small" @click="editor.chain().focus().setTextAlign('left').run()">左对齐</el-button>
      <el-button size="small" @click="editor.chain().focus().setTextAlign('center').run()">居中</el-button>
      <el-button size="small" @click="editor.chain().focus().undo().run()">撤销</el-button>
      <el-button size="small" @click="editor.chain().focus().redo().run()">重做</el-button>
      <span class="counter">{{ characterCount() }} 字</span>
    </div>

    <EditorContent :editor="editor" class="content" />
    <p v-if="uploading > 0" class="status">正在上传 {{ uploading }} 个文件…上传完成后才会插入正文</p>
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
  gap: 6px;
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

.counter {
  margin-left: auto;
  font-size: 12px;
  color: #8a9199;
}

.status {
  margin: 0;
  padding: 6px 12px;
  font-size: 12px;
  color: #6b5316;
  background: #fdf6e3;
}

.content {
  padding: 12px 14px;
  min-height: 220px;
}

.content :deep(.ProseMirror) {
  outline: none;
  min-height: 200px;
}

.content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  height: 0;
  color: #b6bcc4;
  pointer-events: none;
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

.content :deep(table) {
  border-collapse: collapse;
  width: 100%;
  margin: 8px 0;
  table-layout: fixed;
}

.content :deep(th),
.content :deep(td) {
  border: 1px solid #dedbd3;
  padding: 6px 8px;
  text-align: left;
  vertical-align: top;
}

.content :deep(th) {
  background: #faf9f6;
  font-weight: 600;
}

.content :deep(a) {
  color: #c4322b;
}
</style>
