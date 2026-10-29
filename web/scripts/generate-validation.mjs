#!/usr/bin/env node
import { readFile, writeFile } from 'node:fs/promises'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const here = dirname(fileURLToPath(import.meta.url))
const root = resolve(here, '../..')
const schemaPath = resolve(root, 'docs/validation.schema.json')
const outputPath = resolve(root, 'web/packages/api-client/src/validation.ts')

const schema = JSON.parse(await readFile(schemaPath, 'utf8'))
const defs = schema.$defs
if (!defs) throw new Error('validation.schema.json 缺少 $defs')

function required(name) {
  const value = defs[name]
  if (!value) throw new Error('validation.schema.json 缺少 ' + name)
  return value
}

const phone = required('phone')
const email = required('email')
const password = required('password')
const pin = required('pin')
const quantity = required('quantity')
const skuCode = required('skuCode')
const couponCode = required('couponCode')
const price = required('priceAmount')
const contactName = required('contactName')
const reviewMedia = required('reviewMedia')

// 收货地址（ADR-0013/0014）：国家码与区划码是唯一来源，Java 侧 ValidationRules 镜像同一份；
// 长度上限必须与 DB 列宽一致（province/city/district 64、detail 255、postal 16），
// 否则前端放行、写库才报错。
const address = required('address')
const addressProps = address.properties ?? {}
for (const name of address.required ?? []) {
  if (!addressProps[name]) throw new Error('validation.schema.json 缺少 address.' + name)
}
const addressCountry = addressProps.countryCode
const addressRegion = addressProps.regionCode
const addressDetail = addressProps.detail
const addressPostal = addressProps.postalCode
const addressRegionName = addressProps.province

// 素材上传：类型白名单与体积上限（字节）。前端用它做上传前的快速反馈，
// 后端 MediaUploadValidator 用同一份 schema 镜像的常量做权威校验；缺任何字段直接失败，
// 不用默认值顶上（否则 schema 改了前端悄悄沿用旧限制）。
const assetUpload = required('assetUpload')
const uploadProps = assetUpload.properties ?? {}
for (const name of assetUpload.required ?? []) {
  if (!uploadProps[name]) throw new Error('validation.schema.json 缺少 assetUpload.' + name)
}
const imageTypes = uploadProps.imageTypes.items.enum
const videoTypes = uploadProps.videoTypes.items.enum
function bytesRule(name) {
  const rule = uploadProps[name]
  const missing = ['minimum', 'maximum', 'default'].filter(key => typeof rule[key] !== 'number')
  if (missing.length) throw new Error(`assetUpload.${name} 缺少 ${missing.join('/')}`)
  return rule
}
const adminImage = bytesRule('adminImageMaxBytes')
const adminVideo = bytesRule('adminVideoMaxBytes')
const memberImage = bytesRule('memberImageMaxBytes')
const memberVideo = bytesRule('memberVideoMaxBytes')
const multipartCeiling = bytesRule('multipartCeilingBytes')

const content = `/**
 * AUTO-GENERATED from docs/validation.schema.json — do not edit by hand.
 * Run: npm run generate:validation
 */
export const VALIDATION = {
  phonePattern: ${JSON.stringify(phone.pattern)},
  emailPattern: ${JSON.stringify(email.pattern)},
  emailMaxLength: ${email.maxLength},
  passwordMinLength: ${password.minLength},
  pinMinLength: ${pin.minLength},
  pinMaxLength: ${pin.maxLength},
  quantityMin: ${quantity.minimum},
  quantityMax: ${quantity.maximum},
  skuCodePattern: ${JSON.stringify(skuCode.pattern)},
  couponCodePattern: ${JSON.stringify(couponCode.pattern)},
  priceMinimum: ${price.minimum},
  contactNameMaxLength: ${contactName.maxLength},
  addressCountryPattern: ${JSON.stringify(addressCountry.pattern)},
  addressRegionCodePattern: ${JSON.stringify(addressRegion.pattern)},
  addressRegionNameMaxLength: ${addressRegionName.maxLength},
  addressDetailMinLength: ${addressDetail.minLength},
  addressDetailMaxLength: ${addressDetail.maxLength},
  addressPostalCodeMaxLength: ${addressPostal.maxLength},
  reviewMediaMax: ${reviewMedia.maximum},
  assetUpload: {
    imageTypes: ${JSON.stringify(imageTypes)},
    videoTypes: ${JSON.stringify(videoTypes)},
    admin: { maxBytes: ${adminImage.default}, minBytes: ${adminImage.minimum}, maxLimit: ${adminImage.maximum} },
    adminVideo: { maxBytes: ${adminVideo.default}, minBytes: ${adminVideo.minimum}, maxLimit: ${adminVideo.maximum} },
    member: { maxBytes: ${memberImage.default}, minBytes: ${memberImage.minimum}, maxLimit: ${memberImage.maximum} },
    memberVideo: { maxBytes: ${memberVideo.default}, minBytes: ${memberVideo.minimum}, maxLimit: ${memberVideo.maximum} },
    multipartCeilingBytes: ${multipartCeiling.default}
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
`

await writeFile(outputPath, content, 'utf8')
console.log('generated', outputPath)
