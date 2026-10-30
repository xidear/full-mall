#!/usr/bin/env bash
# 本地 E2E 栈：固定使用仓库约定端口（AGENTS §8），不再靠命令行临时改端口。
#
#   API        41280   （.env: API_PORT）
#   admin      41281   （.env: ADMIN_PORT）
#   storefront 41285   （.env: STOREFRONT_PORT）
#
# 为什么需要这个脚本：此前每次"端口被占用"就临时换到 41292/41293/41294，
# 结果 E2E 必须带一堆 E2E_*_BASE 覆盖，跑一次改一次。约定端口是常量，
# 被占用时应当**清掉占用者**，而不是换端口。
#
# 用法：
#   scripts/dev-stack.sh start    # 起 api/worker/admin/storefront（已起则跳过）
#   scripts/dev-stack.sh stop     # 停掉本脚本起的进程与容器
#   scripts/dev-stack.sh status   # 只探测端口与健康状态
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

[ -f .env ] || { echo "缺少 .env：先 cp .env.example .env" >&2; exit 1; }
# shellcheck disable=SC1091
set -a; . ./.env; set +a

API_PORT="${API_PORT:-41280}"
ADMIN_PORT="${ADMIN_PORT:-41281}"
STOREFRONT_PORT="${STOREFRONT_PORT:-41285}"
EXPORT_DIR="$ROOT/server/data/exports"
PID_DIR="${TMPDIR:-/tmp}/fullmall-dev-stack"

probe() { python3 - "$1" <<'PY'
import socket, sys
s = socket.socket(); s.settimeout(0.4)
sys.exit(0 if s.connect_ex(('127.0.0.1', int(sys.argv[1]))) == 0 else 1)
PY
}

wait_port() { # wait_port <port> <seconds> <name>
  local port="$1" limit="$2" name="$3" i=0
  while (( i < limit )); do probe "$port" && return 0; sleep 2; ((i++)); done
  echo "!! $name 在 ${limit}x2s 内没有监听 $port" >&2
  return 1
}

status() {
  echo "== 约定端口 =="
  for pair in "API:$API_PORT" "admin:$ADMIN_PORT" "storefront:$STOREFRONT_PORT"; do
    name="${pair%%:*}"; port="${pair##*:}"
    if probe "$port"; then echo "  $name $port: UP"; else echo "  $name $port: DOWN"; fi
  done
}

start() {
  mkdir -p "$EXPORT_DIR" "$PID_DIR"

  if ! probe "$API_PORT"; then
    echo "启动 API（$API_PORT）"
    docker rm -f fullmall-api-dev >/dev/null 2>&1 || true
    nohup docker run --rm --name fullmall-api-dev --network host --env-file .env \
      -e SERVER_PORT="$API_PORT" -e SPRING_PROFILES_ACTIVE=api,dev \
      -e APP_EXPORT_STORAGE_DIR=/repo/server/data/exports \
      -v "$ROOT":/repo -v /tmp/m2cache:/root/.m2 -w /repo/server \
      maven:3.9-eclipse-temurin-21 mvn -B -q spring-boot:run > "$PID_DIR/api.log" 2>&1 &
  fi
  wait_port "$API_PORT" 45 API

  if ! probe "$API_PORT"; then :; fi
  if ! docker ps --format '{{.Names}}' | grep -qx fullmall-worker-dev; then
    echo "启动 worker"
    nohup docker run --rm --name fullmall-worker-dev --network host --env-file .env \
      -e SPRING_PROFILES_ACTIVE=worker,dev \
      -e APP_EXPORT_STORAGE_DIR=/repo/server/data/exports \
      -v "$ROOT":/repo -v /tmp/m2cache:/root/.m2 -w /repo/server \
      maven:3.9-eclipse-temurin-21 mvn -B -q spring-boot:run > "$PID_DIR/worker.log" 2>&1 &
  fi

  if ! probe "$ADMIN_PORT"; then
    echo "启动 admin（$ADMIN_PORT）"
    (cd web/apps/admin && VITE_API_BASE="http://localhost:$API_PORT" \
      nohup npx vite --host 0.0.0.0 --port "$ADMIN_PORT" --strictPort > "$PID_DIR/admin.log" 2>&1 &
      echo $! > "$PID_DIR/admin.pid")
  fi
  wait_port "$ADMIN_PORT" 30 admin

  if ! probe "$STOREFRONT_PORT"; then
    echo "启动 storefront（$STOREFRONT_PORT）"
    (cd web/apps/storefront && NUXT_PUBLIC_API_BASE="http://127.0.0.1:$API_PORT" \
      NUXT_API_INTERNAL_BASE="http://127.0.0.1:$API_PORT" \
      nohup npx nuxt dev --host 0.0.0.0 --port "$STOREFRONT_PORT" > "$PID_DIR/storefront.log" 2>&1 &
      echo $! > "$PID_DIR/storefront.pid")
  fi
  wait_port "$STOREFRONT_PORT" 60 storefront

  status
  echo
  echo "跑 E2E（无需任何 E2E_* 覆盖）：  cd web && npx playwright test --project=chromium"
}

stop() {
  docker rm -f fullmall-api-dev fullmall-worker-dev >/dev/null 2>&1 || true
  for name in admin storefront; do
    if [[ -f "$PID_DIR/$name.pid" ]]; then
      kill "$(cat "$PID_DIR/$name.pid")" 2>/dev/null || true
      rm -f "$PID_DIR/$name.pid"
    fi
  done
  echo "已停止（端口按约定保留给下次启动）"
}

case "${1:-status}" in
  start) start ;;
  stop) stop ;;
  status) status ;;
  *) echo "用法: $0 {start|stop|status}" >&2; exit 2 ;;
esac
