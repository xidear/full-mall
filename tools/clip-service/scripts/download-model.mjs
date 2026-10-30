/**
 * 从 hf-mirror 下载 CLIP 模型文件到 ./models（国内直连 huggingface.co 会超时）。
 *
 * 只下 CPU 推理实际用到的那几个：量化版视觉/文本模型 + 分词与预处理配置。
 */
import { mkdir, writeFile } from 'node:fs/promises'
import { dirname } from 'node:path'

const base = process.env.CLIP_MIRROR || 'https://hf-mirror.com'
const model = process.env.CLIP_MODEL || 'Xenova/clip-vit-base-patch32'
const root = new URL('./models/' + model + '/', import.meta.url).pathname

const files = [
  'config.json',
  'preprocessor_config.json',
  'tokenizer.json',
  'tokenizer_config.json',
  'special_tokens_map.json',
  'onnx/vision_model_quantized.onnx',
  'onnx/text_model_quantized.onnx',
  'onnx/model_quantized.onnx'
]

for (const file of files) {
  const response = await fetch(base + '/' + model + '/resolve/main/' + file)
  if (!response.ok) {
    throw new Error(file + ' 下载失败 HTTP ' + response.status)
  }
  const buffer = Buffer.from(await response.arrayBuffer())
  const target = root + file
  await mkdir(dirname(target), { recursive: true })
  await writeFile(target, buffer)
  console.log(file, (buffer.length / 1024 / 1024).toFixed(1) + 'MB')
}
console.log('模型已就绪：' + root)
