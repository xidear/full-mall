/**
 * 非安全上下文（例如 http://192.168.1.15:41285）没有 crypto.randomUUID，
 * 但 crypto.getRandomValues 在 HTTP 下可用。统一走这里，避免加购/结算直接崩。
 */
export function randomUuid(): string {
  const webCrypto = globalThis.crypto
  if (webCrypto?.randomUUID) {
    return webCrypto.randomUUID()
  }
  if (!webCrypto?.getRandomValues) {
    throw new Error('当前浏览器不支持 Web Crypto，无法生成幂等键')
  }
  const bytes = webCrypto.getRandomValues(new Uint8Array(16))
  bytes[6] = (bytes[6] & 0x0f) | 0x40
  bytes[8] = (bytes[8] & 0x3f) | 0x80
  const hex = [...bytes].map(value => value.toString(16).padStart(2, '0')).join('')
  return [hex.slice(0, 8), hex.slice(8, 12), hex.slice(12, 16), hex.slice(16, 20), hex.slice(20)].join('-')
}
