#!/usr/bin/env bash
# 一键验证：后端测试 → 打包 → 前端构建 → 接口对齐 → 真浏览器 E2E。
#
# 用法：
#   scripts/verify.sh            # 全部
#   scripts/verify.sh --no-e2e   # 跳过 E2E（不需要浏览器与前端服务）
#
# 设计要点：
#   - set -euo pipefail：任何一步失败立即退出，不"看起来成功"；
#   - 后台进程记录 PID，trap 里按 PID 清理，不用模糊匹配 kill；
#   - 后端用**打包后的 jar**运行：启动失败会明确报端口占用，避免测试到旧代码。
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUN_E2E=1
[[ "${1:-}" == "--no-e2e" ]] && RUN_E2E=0

PG_PORT=55432
API_PORT=8080
WEB_PORT=3000
CONSOLE_PORTS=(5273 5274 5275 5276)
CONSOLES=(admin store-console supplier-portal support-console)

PIDS=()
cleanup() {
  for pid in "${PIDS[@]:-}"; do
    [[ -n "${pid:-}" ]] && kill -9 "$pid" 2>/dev/null || true
  done
}
trap cleanup EXIT

wait_for() { # wait_for <url> <seconds> <name>
  local url="$1" limit="$2" name="$3" i=0
  while (( i < limit )); do
    if curl_ok "$url"; then return 0; fi
    sleep 1; ((i++))
  done
  echo "!! $name 在 ${limit}s 内未就绪: $url" >&2
  return 1
}

curl_ok() { # 容器内可能没有 curl，用 python 探测
  python3 - "$1" <<'PY'
import sys, urllib.request
try:
    urllib.request.urlopen(sys.argv[1], timeout=5)
except Exception:
    sys.exit(1)
PY
}

echo "== 1/6 后端测试 =="
(cd "$ROOT/server" && mvn -B test)

echo "== 2/6 后端打包 =="
(cd "$ROOT/server" && mvn -B -q -DskipTests package)

echo "== 3/6 前端构建 =="
(cd "$ROOT/web" && npm ci --no-audit --no-fund --cache /tmp/npm-cache && npm run build)

echo "== 4/6 接口对齐 =="
python3 "$ROOT/scripts/check-api-alignment.py"

echo "== 5/6 帮助文档覆盖 =="
python3 scripts/check-help-coverage.py

if (( RUN_E2E == 0 )); then
  echo "== 跳过 E2E（--no-e2e）=="
  exit 0
fi

echo "== 6/6 E2E =="
cd "$ROOT/server"
# 1) 嵌入式 PostgreSQL（复用测试依赖，容器内无需 Docker）
mvn -B -q dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt -Dmdep.includeScope=test
CP="target/test-classes:target/classes:$(cat /tmp/cp.txt)"
java -cp "$CP" com.fullmall.test.EmbeddedPgServer > /tmp/fullmall-pg.log 2>&1 &
PIDS+=($!)
# PG 不是 HTTP 服务，按日志里的就绪标记等待（不要用 HTTP 探测，那必然失败并产生误导性输出）
pg_ready=0
for _ in $(seq 1 60); do
  if grep -q PG_READY /tmp/fullmall-pg.log 2>/dev/null; then pg_ready=1; break; fi
  sleep 1
done
(( pg_ready == 1 )) || { echo "!! PostgreSQL 未就绪，见 /tmp/fullmall-pg.log" >&2; exit 1; }

# 2) 后端 jar（启动失败会明确报错，而不是静默复用旧进程）
SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:${PG_PORT}/postgres" \
SPRING_DATASOURCE_USERNAME=postgres SPRING_DATASOURCE_PASSWORD="" \
SPRING_PROFILES_ACTIVE=dev \
  java -jar target/fullmall-server-0.1.0-SNAPSHOT.jar > /tmp/fullmall-api.log 2>&1 &
PIDS+=($!)
wait_for "http://localhost:${API_PORT}/api/v1/products" 90 "后端" || { tail -30 /tmp/fullmall-api.log >&2; exit 1; }

# 3) 前端（storefront 生产构建 + 四个控制台 dev）
cd "$ROOT/web"
NUXT_PUBLIC_API_BASE="http://localhost:${API_PORT}" PORT="${WEB_PORT}" \
  node apps/storefront/.output/server/index.mjs > /tmp/fullmall-web.log 2>&1 &
PIDS+=($!)
for i in "${!CONSOLES[@]}"; do
  npm run dev:e2e --workspace "@fullmall/${CONSOLES[$i]}" > "/tmp/fullmall-${CONSOLES[$i]}.log" 2>&1 &
  PIDS+=($!)
done
wait_for "http://localhost:${WEB_PORT}/" 60 "storefront" || exit 1
for port in "${CONSOLE_PORTS[@]}"; do
  wait_for "http://localhost:${port}/" 60 "console:${port}" || exit 1
done

npx playwright test --project=chromium
echo "== 全部通过 =="
