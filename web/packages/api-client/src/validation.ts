/**
 * AUTO-GENERATED from docs/validation.schema.json — do not edit by hand.
 * Run: npm run generate:validation
 */
export const VALIDATION = {
  phonePattern: "^\\+?[0-9]{6,20}$",
  passwordMinLength: 8,
  pinMinLength: 4,
  pinMaxLength: 12,
  quantityMin: 1,
  quantityMax: 99,
  skuCodePattern: "^[A-Za-z0-9._-]{1,64}$",
  couponCodePattern: "^[A-Z0-9][A-Z0-9_-]{1,63}$",
  priceMinimum: 1,
  contactNameMaxLength: 64,
  reviewMediaMax: 9,
  assetUpload: {
    imageTypes: ["image/png","image/jpeg","image/webp","image/gif"],
    videoTypes: ["video/mp4","video/webm","video/quicktime"],
    admin: { maxBytes: 5242880, minBytes: 102400, maxLimit: 20971520 },
    adminVideo: { maxBytes: 10485760, minBytes: 102400, maxLimit: 67108864 },
    member: { maxBytes: 2097152, minBytes: 102400, maxLimit: 5242880 },
    memberVideo: { maxBytes: 5242880, minBytes: 102400, maxLimit: 20971520 },
    multipartCeilingBytes: 67108864
  }
} as const
type AssetUpload = typeof VALIDATION.assetUpload

/** 图片类型白名单（Tika 嗅探结果）。SVG 故意不在其中。 */
export const UPLOAD_IMAGE_TYPES: readonly string[] = VALIDATION.assetUpload.imageTypes
/** 视频类型白名单（Tika 嗅探结果）。 */
export const UPLOAD_VIDEO_TYPES: readonly string[] = VALIDATION.assetUpload.videoTypes

/** 上传场景：管理端（商品/素材）与顾客端（评价晒图）上限不同。 */
export type UploadScope = 'admin' | 'member'

/** 默认上限（字节）：仅用于后端未下发运行时配置时的兜底展示，真实上限以服务端返回为准。 */
export function defaultUploadLimitBytes(scope: UploadScope, kind: 'image' | 'video'): number {
  const rules: AssetUpload = VALIDATION.assetUpload
  if (kind === 'video') return scope === 'member' ? rules.memberVideo.maxBytes : rules.adminVideo.maxBytes
  return scope === 'member' ? rules.member.maxBytes : rules.admin.maxBytes
}
