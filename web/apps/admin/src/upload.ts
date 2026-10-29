import { VALIDATION } from '@fullmall/api-client'
import { api } from './api'

/**
 * 管理端素材上传的唯一入口。
 *
 * <p>三件事在这里一次做完，页面不再各自拼 FormData、各自写 accept、各自猜上限：
 * 1. 上限与类型白名单来自后端 `/admin/v1/media/limits`（后台可配，改完刷新页面即变），
 *    不用前端的旧常量——AGENTS.md §9 禁止同一规则写两份；
 * 2. iPhone 直传的 HEIC/HEIF 先在本机转成 JPEG：浏览器解码不了 HEIC，
 *    服务端也不接收未转码的 HEIC（会返回 ASSET_HEIC_NOT_CONVERTED）；
 * 3. 本机预检只做"更快给出提示"，最终以服务端判定为准。
 */

export interface UploadLimits {
  imageTypes: string[]
  videoTypes: string[]
  imageMaxBytes: number
  videoMaxBytes: number
}

export interface UploadedMedia {
  url: string
  key: string
  bytes: number
  provider: string
  purpose: string
  kind: 'IMAGE' | 'VIDEO'
  contentType: string
  assetId: string | null
}

let limitsRequest: Promise<UploadLimits> | null = null

/** 读取当前生效的上传规则；失败时清缓存，避免一次网络抖动把错误值长期钉住。 */
export function loadUploadLimits(force = false): Promise<UploadLimits> {
  if (!limitsRequest || force) {
    limitsRequest = api.get<UploadLimits>('/admin/v1/media/limits').catch((cause: unknown) => {
      limitsRequest = null
      throw cause
    })
  }
  return limitsRequest
}

export function acceptAttribute(limits: UploadLimits): string {
  return [...limits.imageTypes, ...limits.videoTypes].join(',')
}

export function megabytes(bytes: number): string {
  return (bytes / 1024 / 1024).toFixed(1)
}

export function isHeicFile(file: File): boolean {
  const type = (file.type || '').toLowerCase()
  return type === 'image/heic' || type === 'image/heif' || /\.hei[cf]$/i.test(file.name)
}

/**
 * HEIC/HEIF → JPEG。
 *
 * <p>用 heic2any（MIT，2.7MB，wasm 解码 libheif）：浏览器端转换避免给服务端装 libheif 原生依赖。
 * 动态 import 让没用过 HEIC 的用户不必下载这份 wasm。
 */
export async function convertHeicToJpeg(file: File): Promise<File> {
  // WebAssembly 是企业策略里最容易被禁用的一项（浏览器 flag、组策略、严格 CSP）。
  // 这里提前给出"该怎么办"，而不是让 heic2any 抛一段英文堆栈给运营看。
  if (typeof WebAssembly === 'undefined') {
    throw new Error('这台电脑的浏览器禁用了 WebAssembly，无法自动转换 HEIC。'
      + '请在手机相册里「导出/另存为 JPEG」后再上传，或改用新版 Chrome/Edge/Safari。')
  }
  let converted: Blob | Blob[]
  try {
    const { default: heic2any } = await import('heic2any')
    converted = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.92 }) as Blob | Blob[]
  } catch (cause) {
    throw new Error('HEIC 自动转换失败（浏览器策略或文件损坏）：请把照片导出为 JPEG 后重试。'
      + '原始错误：' + (cause instanceof Error ? cause.message : String(cause)))
  }
  const blob = Array.isArray(converted) ? converted[0] : converted
  if (!blob) {
    throw new Error('HEIC 转换结果为空：请把照片导出为 JPEG 后重试')
  }
  return new File([blob], file.name.replace(/\.hei[cf]$/i, '.jpg'), { type: 'image/jpeg' })
}

/** 上传前把文件整理成服务端可接受的形态（目前只有 HEIC 需要处理）。 */
export async function prepareForUpload(file: File): Promise<File> {
  return isHeicFile(file) ? convertHeicToJpeg(file) : file
}

/** 本机预检：只用于快速反馈，文案与后端错误码含义保持一致。 */
export function precheck(file: File, limits: UploadLimits): string | null {
  const type = (file.type || '').toLowerCase()
  const isImage = limits.imageTypes.includes(type)
  const isVideo = limits.videoTypes.includes(type)
  if (!isImage && !isVideo) {
    return `不支持的文件类型：${type || '未知'}。允许图片 ${limits.imageTypes.length} 种、视频 ${limits.videoTypes.length} 种`
  }
  const limit = isVideo ? limits.videoMaxBytes : limits.imageMaxBytes
  if (file.size > limit) {
    return `文件 ${megabytes(file.size)}MB，超过${isVideo ? '视频' : '图片'}上限 ${megabytes(limit)}MB`
  }
  return null
}

/**
 * 上传一个文件。
 *
 * @param purpose PRODUCT / SPEC / VIDEO / OTHER_SYSTEM（OTHER_SYSTEM 不进素材库）
 */
export async function uploadMedia(file: File, purpose = 'PRODUCT'): Promise<UploadedMedia> {
  const limits = await loadUploadLimits()
  const prepared = await prepareForUpload(file)
  const problem = precheck(prepared, limits)
  if (problem) {
    throw new Error(problem)
  }
  const form = new FormData()
  form.append('file', prepared)
  return api.postForm<UploadedMedia>(`/admin/v1/media?purpose=${encodeURIComponent(purpose)}`, form)
}

/** 允许上传的最大视频体积（用于文案），取不到配置时退回 schema 默认值，并明确标注是默认值。 */
export function fallbackVideoLimitMb(): string {
  return megabytes(VALIDATION.assetUpload.adminVideo.maxBytes)
}
