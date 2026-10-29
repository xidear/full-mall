/**
 * 地址模型（ADR-0014）：countryCode 是 Destination 轴，regionCode 是最细一级区划码。
 * 前端只用它做表单承载与即时提示；权威校验在后端（AddressService + address_format）。
 */
export interface AddressModel {
  countryCode: string
  regionCode: string
  province: string
  city: string
  district: string
  detail: string
  postalCode: string
}

export function emptyAddress(countryCode = ''): AddressModel {
  return { countryCode, regionCode: '', province: '', city: '', district: '', detail: '', postalCode: '' }
}

/** 用于"至少填了国家 + 详细地址"的快速判断；完整规则以接口返回的 address_format 为准。 */
export function hasMinimalAddress(address: AddressModel): boolean {
  return !!address.countryCode.trim() && !!address.detail.trim()
}
