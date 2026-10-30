/**
 * 开发机专用的多模态（CLIP）embedding 服务。
 *
 * <p>为什么单独一个进程：图像向量属于"可选能力"，客户服务器不一定装得下模型；
 * 主程序只按「AI 供应商」表单里配置的 endpoint 调用，没配就不影响其它功能。
 *
 * <p>模型：Xenova/clip-vit-base-patch32（ONNX 量化版，CPU 可跑，约 150MB）。
 * 模型文件预先用 hf-mirror 下到 ./models（见 README），运行时不联网。
 *
 * <p>实现要点：CLIP 的文本/图像是两个独立子模型，必须分别用
 * CLIPTextModelWithProjection / CLIPVisionModelWithProjection；
 * 用通用 feature-extraction 管道会走视觉模型，喂文本会报 "Missing pixel_values"。
 *
 * <p>接口（与主程序 ClipEmbeddingClient 对应）：
 * GET  /health                        → { ok, model, dim }
 * POST /embed/text   { text }         → { vector: number[512] }
 * POST /embed/image  { url | base64 } → { vector: number[512] }
 */
import http from 'node:http'
import {
  env, AutoTokenizer, AutoProcessor, RawImage,
  CLIPTextModelWithProjection, CLIPVisionModelWithProjection
} from '@xenova/transformers'

env.allowLocalModels = true
env.useBrowserCache = false
// 模型走本地目录：库自带的下载器在国内经常连 huggingface.co 超时
env.localModelPath = new URL('./models/', import.meta.url).pathname
env.allowRemoteModels = process.env.CLIP_ALLOW_REMOTE === '1'
env.remoteHost = process.env.CLIP_REMOTE_HOST || 'https://hf-mirror.com'
env.remotePathTemplate = '{model}/resolve/{revision}/'

// 端口见 AGENTS.md §8 / docs/DEVELOPMENT_PORTS.md：41280–41299 段内的 41290（可选服务）
const PORT = Number(process.env.CLIP_PORT || 41290)
const MODEL = process.env.CLIP_MODEL || 'Xenova/clip-vit-base-patch32'
const DIM = 512

let tokenizer = null
let textModel = null
let processor = null
let visionModel = null

async function loadText() {
  if (!textModel) {
    tokenizer = await AutoTokenizer.from_pretrained(MODEL)
    textModel = await CLIPTextModelWithProjection.from_pretrained(MODEL, { quantized: true })
  }
}

async function loadVision() {
  if (!visionModel) {
    processor = await AutoProcessor.from_pretrained(MODEL)
    visionModel = await CLIPVisionModelWithProjection.from_pretrained(MODEL, { quantized: true })
  }
}

/** CLIP 两个子模型的输出未归一化，余弦相似度前必须自己 L2 归一化。 */
function normalize(tensor) {
  const values = Array.from(tensor.data, Number)
  const norm = Math.hypot(...values) || 1
  return values.map((v) => v / norm)
}

async function embedText(text) {
  if (!text || !text.trim()) {
    throw new Error('文本为空')
  }
  await loadText()
  const inputs = tokenizer([text], { padding: true, truncation: true })
  const output = await textModel(inputs)
  return normalize(output.text_embeds)
}

async function embedImage(input) {
  await loadVision()
  const image = typeof input === 'string'
    ? await RawImage.read(input)
    : await RawImage.fromBlob(new Blob([input], { type: 'image/png' }))
  const inputs = await processor(image)
  const output = await visionModel(inputs)
  return normalize(output.image_embeds)
}

function readBody(request) {
  return new Promise((resolve, reject) => {
    const chunks = []
    request.on('data', (chunk) => chunks.push(chunk))
    request.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')))
    request.on('error', reject)
  })
}

function send(response, status, payload) {
  response.writeHead(status, { 'content-type': 'application/json; charset=utf-8' })
  response.end(JSON.stringify(payload))
}

const server = http.createServer(async (request, response) => {
  try {
    if (request.method === 'GET' && request.url === '/health') {
      return send(response, 200, { ok: true, model: MODEL, dim: DIM })
    }
    if (request.method === 'POST' && (request.url === '/embed/text' || request.url === '/embed/image')) {
      const raw = await readBody(request)
      const body = raw ? JSON.parse(raw) : {}
      const started = Date.now()
      const vector = request.url === '/embed/text'
        ? await embedText(String(body.text || ''))
        : await embedImage(body.base64 ? Buffer.from(String(body.base64), 'base64') : String(body.url || ''))
      // 空向量是调用方 bug，不能返回 512 个 0 假装成功
      if (!vector.length || vector.every((v) => v === 0)) {
        return send(response, 422, { error: 'embedding 为空：输入内容无效' })
      }
      return send(response, 200, { vector, dim: vector.length, ms: Date.now() - started })
    }
    send(response, 404, { error: 'not found' })
  } catch (error) {
    console.error('[clip] 请求失败', error)
    send(response, 500, { error: error instanceof Error ? error.message : String(error) })
  }
})

server.listen(PORT, '0.0.0.0', () => {
  console.log('[clip] listening on ' + PORT + ' model=' + MODEL)
})
