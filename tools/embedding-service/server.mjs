/**
 * 本机 CPU embedding 服务（OpenAI 兼容）。
 *
 * <p>为什么需要：主程序的语义检索走 `openai-embedding` 协议，但客户/开发机常常没有
 * 可用的向量服务。这里用 transformers.js（ONNX）在 CPU 上跑一个中文小模型，
 * 对外只暴露 `POST /v1/embeddings`，主程序按普通供应商配置即可，不需要改 Java 代码。
 *
 * <p>模型：Xenova/bge-small-zh-v1.5（量化 ONNX，约 23MB，512 维，中文检索）。
 * 首次启动会从 hf-mirror 下载到 `./models`，之后离线可用。
 *
 * <p>接口：
 * GET  /health          → { ok, model, dim, loaded }
 * GET  /v1/models       → OpenAI 风格模型列表
 * POST /v1/embeddings   → { object:"list", data:[{object:"embedding",index,embedding:[...]}], model }
 * POST /embeddings      → 同上（兼容只写 /embeddings 的客户端）
 */
import http from 'node:http'
import { env, pipeline } from '@xenova/transformers'

env.allowLocalModels = true
env.useBrowserCache = false
// 模型下载/缓存都放仓库内 ./models：默认 cacheDir 是 ~/.cache，迁移机器时会“看起来能跑、其实没模型”
env.localModelPath = new URL('./models/', import.meta.url).pathname
env.cacheDir = env.localModelPath
// 默认允许首次联网下载；离线部署设 EMBEDDING_ALLOW_REMOTE=0，缺模型时启动失败而不是静默空向量
env.allowRemoteModels = process.env.EMBEDDING_ALLOW_REMOTE !== '0'
env.remoteHost = process.env.EMBEDDING_REMOTE_HOST || 'https://hf-mirror.com'
env.remotePathTemplate = '{model}/resolve/{revision}/'

const PORT = Number(process.env.EMBEDDING_PORT || 41291)
const MODEL = process.env.EMBEDDING_MODEL || 'Xenova/bge-small-zh-v1.5'
const DIM = Number(process.env.EMBEDDING_DIM || 512)

let extractor = null
let loading = null

async function getExtractor() {
  if (extractor) return extractor
  if (!loading) {
    // BGE 系列用 CLS pooling；mean 会让同义句相似度明显下降
    loading = pipeline('feature-extraction', MODEL, { quantized: true })
      .then((pipe) => { extractor = pipe; return pipe })
      .catch((error) => { loading = null; throw error })
  }
  return loading
}

async function embed(texts) {
  const pipe = await getExtractor()
  const output = await pipe(texts, { pooling: 'cls', normalize: true })
  const rows = output.tolist()
  return Array.isArray(rows[0]) ? rows : [rows]
}

function json(res, status, body) {
  const payload = JSON.stringify(body)
  res.writeHead(status, {
    'Content-Type': 'application/json; charset=utf-8',
    'Content-Length': Buffer.byteLength(payload)
  })
  res.end(payload)
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = ''
    req.on('data', (chunk) => {
      data += chunk
      if (data.length > 1_000_000) {
        reject(new Error('请求体超过 1MB'))
        req.destroy()
      }
    })
    req.on('end', () => resolve(data))
    req.on('error', reject)
  })
}

const server = http.createServer(async (req, res) => {
  try {
    if (req.method === 'GET' && req.url === '/health') {
      return json(res, 200, { ok: true, model: MODEL, dim: DIM, loaded: Boolean(extractor) })
    }
    if (req.method === 'GET' && req.url === '/v1/models') {
      return json(res, 200, {
        object: 'list',
        data: [{ id: MODEL, object: 'model', owned_by: 'local-cpu' }]
      })
    }
    if (req.method === 'POST' && (req.url === '/v1/embeddings' || req.url === '/embeddings')) {
      const body = JSON.parse(await readBody(req) || '{}')
      const input = body.input
      const texts = Array.isArray(input) ? input : [input]
      if (texts.length === 0 || texts.some((text) => typeof text !== 'string' || !text.trim())) {
        return json(res, 400, { error: { message: 'input 不能为空', type: 'invalid_request_error' } })
      }
      const vectors = await embed(texts)
      return json(res, 200, {
        object: 'list',
        model: body.model || MODEL,
        data: vectors.map((embedding, index) => ({
          object: 'embedding',
          index,
          embedding
        })),
        usage: { prompt_tokens: 0, total_tokens: 0 }
      })
    }
    return json(res, 404, { error: { message: 'not found' } })
  } catch (error) {
    // 失败必须带原因：主程序会把 HTTP 非 2xx 当成"向量服务不可用"，不能返回空向量
    console.error('[embedding]', error)
    return json(res, 500, { error: { message: error.message || String(error) } })
  }
})

server.listen(PORT, '0.0.0.0', () => {
  console.log(`embedding service listening on :${PORT}, model=${MODEL}, dim=${DIM}`)
  // 预热：避免第一次业务请求承担 5-15 秒模型加载超时
  getExtractor().then(() => console.log('[embedding] model ready')).catch((error) => {
    console.error('[embedding] model load failed:', error.message)
    process.exitCode = 1
  })
})