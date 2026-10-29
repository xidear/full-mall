/**
 * 顾客端媒体上传（评价晒图/晒视频）。
 *
 * 上限与类型白名单来自后端 `/api/v1/media/limits`（后台可配），不在前端写死；
 * HEIC/HEIF 先在本机转 JPEG——浏览器显示不了 HEIC，服务端也会要求先转码。
 */

export interface MemberUploadLimits {
  imageTypes: string[]
  videoTypes: string[]
  imageMaxBytes: number
  videoMaxBytes: number
}

export interface UploadedMemberMedia {
  url: string
  key: string
  bytes: number
  provider: string
  kind: 'IMAGE' | 'VIDEO'
  contentType: string
}

export function isHeicFile(file: File): boolean {
  const type = (file.type || '').toLowerCase()
  return type === 'image/heic' || type === 'image/heif' || /\.hei[cf]$/i.test(file.name)
}

/** heic2any（MIT）：浏览器端 wasm 解码，动态加载，不用 HEIC 的用户不会下载这 1.3MB。 */
export async function convertHeicToJpeg(file: File): Promise<File> {
  // 顾客端更需要"人话"：旧浏览器/被禁 WebAssembly 时直接告诉用户怎么导出 JPEG。
  if (typeof WebAssembly === 'undefined') {
    throw new Error('当前浏览器无法自动转换 HEIC 照片，请在相册里导出为 JPEG 后再上传')
  }
  let converted: Blob | Blob[]
  try {
    const { default: heic2any } = await import('heic2any')
    converted = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.92 }) as Blob | Blob[]
  } catch (cause) {
    throw new Error('HEIC 照片转换失败，请导出为 JPEG 后重试'
      + (cause instanceof Error ? `（${cause.message}）` : ''))
  }
  const blob = Array.isArray(converted) ? converted[0] : converted
  if (!blob) {
    throw new Error('HEIC 照片转换失败，请导出为 JPEG 后重试')
  }
  return new File([blob], file.name.replace(/\.hei[cf]$/i, '.jpg'), { type: 'image/jpeg' })
}

export async function prepareForUpload(file: File): Promise<File> {
  return isHeicFile(file) ? convertHeicToJpeg(file) : file
}

export function megabytes(bytes: number): string {
  return (bytes / 1024 / 1024).toFixed(1)
}

/** 上传前的本机预检：只为了少一次失败往返，权威判定仍在服务端。 */
export function precheckMemberUpload(file: File, limits: MemberUploadLimits): string | null {
  const type = (file.type || '').toLowerCase()
  const isVideo = limits.videoTypes.includes(type)
  if (!limits.imageTypes.includes(type) && !isVideo) {
    return '只能上传常见图片（png/jpeg/webp/gif）或视频（mp4/webm/mov）'
  }
  const limit = isVideo ? limits.videoMaxBytes : limits.imageMaxBytes
  if (file.size > limit) {
    return `文件 ${megabytes(file.size)}MB，超过${isVideo ? '视频' : '图片'}上限 ${megabytes(limit)}MB`
  }
  return null
}

export async function fetchMemberUploadLimits(): Promise<MemberUploadLimits> {
  return await $fetch<MemberUploadLimits>('/api/v1/media/limits')
}

export async function uploadMemberMedia(file: File, token: string,
                                        limits: MemberUploadLimits): Promise<UploadedMemberMedia> {
  const prepared = await prepareForUpload(file)
  const problem = precheckMemberUpload(prepared, limits)
  if (problem) {
    throw new Error(problem)
  }
  const form = new FormData()
  form.append('file', prepared)
  return await $fetch<UploadedMemberMedia>('/api/v1/media', {
    method: 'POST',
    body: form,
    headers: { Authorization: `Bearer ${token}` }
  })
}
