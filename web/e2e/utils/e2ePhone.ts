/**
 * e2e 专用手机号：统一 199 前缀。
 *
 * <p>为什么必须统一：测试会真实注册会员、下单。没有可识别的号段，
 * 清理只能"按时间猜"，容易误删真实数据；有了 199 前缀，
 * 清理脚本（scripts/e2e-teardown.mjs）只碰这一类，范围可控。
 */
export function e2ePhone(): string {
  // 后 8 位取时间戳尾部，保证同一轮里多次注册也不冲突
  return '199' + String(Date.now()).slice(-8)
}
