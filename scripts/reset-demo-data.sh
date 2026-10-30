#!/usr/bin/env bash
# 确保演示数据存在（幂等）：调用后台自己的 /admin/v1/setup/data（mode=DEMO）。
#
# 语义说明（别误解）：
# - DEMO 只在库为空/首次配置时播种；库已有业务数据时不会删除任何东西，只把 data-mode 标记为 DEMO。
#   因此本脚本不是"清库重来"。
# - 真要把演示数据恢复出厂，用 make db-reset（重建数据库卷，不可逆）。
# - 生产 profile 下 DEMO 返回 409 DEMO_SEEDER_UNAVAILABLE，不会假装装好了。
set -euo pipefail

API="${API_BASE:-http://127.0.0.1:41280}"
USERNAME="${ADMIN_USERNAME:-admin}"
PASSWORD="${ADMIN_PASSWORD:-Admin@123456}"

node --input-type=module - "$API" "$USERNAME" "$PASSWORD" <<'NODEEOF'
const [api, username, password] = process.argv.slice(2)
const post = async (path, body, token) => {
  const response = await fetch(api + path, {
    method: 'POST',
    headers: { 'content-type': 'application/json', ...(token ? { authorization: 'Bearer ' + token } : {}) },
    body: JSON.stringify(body)
  })
  const text = await response.text()
  if (!response.ok) {
    throw new Error(path + ' 失败 ' + response.status + ': ' + text)
  }
  return text ? JSON.parse(text) : null
}
const captcha = await (await fetch(api + '/admin/v1/auth/captcha')).json()
const operands = (captcha.question.match(/\d+/g) ?? []).map(Number)
const login = await post('/admin/v1/auth/login', {
  username, password, captchaId: captcha.captchaId, captchaAnswer: String(operands[0] + operands[1])
})
const status = await post('/admin/v1/setup/data', { mode: 'DEMO' }, login.token)
console.log('数据模式：' + status.mode + '，商品 ' + status.products + ' / 订单 ' + status.orders + ' / 会员 ' + status.members)
console.log('（已有数据不会被删除；要恢复出厂请用 make db-reset）')
NODEEOF
