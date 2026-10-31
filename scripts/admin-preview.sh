#!/usr/bin/env bash
# 15052 → 192.168.1.15:41281 的生产构建服务（vite preview），与 dev 区分开。
#
# 为什么需要：dev-stack 起的是 Vite dev（源码实时编译），15052 是运营入口，
# 必须跑构建产物；两者都占 41281，谁先起谁生效，容易误判"改动没生效"。
#
# 用法：
#   scripts/admin-preview.sh start    # 构建 + 启动（已占用则先停旧的 preview）
#   scripts/admin-preview.sh restart  # 强制重建 + 重启
#   scripts/admin-preview.sh stop
#   scripts/admin-preview.sh status
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT/web"
PID_FILE="${TMPDIR:-/tmp}/fullmall-admin-preview.pid"
LOG_FILE="${TMPDIR:-/tmp}/fullmall-admin-preview.log"
PORT=41281
PATTERN="vite preview --host 0.0.0.0 --port ${PORT}"

probe() {
  python3 - "$PORT" <<'PY'
import socket, sys
s = socket.socket(); s.settimeout(0.4)
sys.exit(0 if s.connect_ex(('127.0.0.1', int(sys.argv[1]))) == 0 else 1)
PY
}

stop_server() {
  # 只杀 preview（dev 的进程命令行是 `vite --host ...`，不会命中）
  local pids
  pids="$(pgrep -f "$PATTERN" || true)"
  if [[ -n "$pids" ]]; then
    # shellcheck disable=SC2086
    kill $pids 2>/dev/null || true
    sleep 1
  fi
  rm -f "$PID_FILE"
}

build() {
  npm run build --workspace @fullmall/admin
}

start() {
  if probe && [[ ! -f "$PID_FILE" ]]; then
    echo "41281 已被其它进程占用（可能还是 Vite dev）；先确认并停掉，再执行本脚本" >&2
    python3 "$ROOT/scripts/port-owners.mjs" 2>/dev/null || true
    exit 1
  fi
  stop_server
  build
  # setsid -f：再 fork 一次彻底脱离当前 shell。普通 `setsid ... &` 在本脚本里会让
  # bash 仍把它当子进程等待（do_wait），脚本不退出、调用方以为卡死。
  (cd apps/admin && setsid -f npx vite preview --host 0.0.0.0 --port "$PORT" --strictPort \
      > "$LOG_FILE" 2>&1 < /dev/null)
  for _ in $(seq 1 20); do
    probe && { pgrep -f "$PATTERN" | head -1 > "$PID_FILE" || true;
               echo "admin preview 已启动：http://127.0.0.1:${PORT}（日志 $LOG_FILE）"; return 0; }
    sleep 1
  done
  echo "启动失败，查看日志：$LOG_FILE" >&2
  tail -20 "$LOG_FILE" >&2 || true
  exit 1
}

status() {
  if probe; then
    echo "41281：UP"
    [[ -f "$PID_FILE" ]] && echo "preview pid：$(cat "$PID_FILE")"
  else
    echo "41281：DOWN"
    exit 1
  fi
}

case "${1:-status}" in
  start) start ;;
  restart) stop_server; start ;;
  stop) stop_server; echo "已停止 preview" ;;
  status) status ;;
  *) echo "用法: $0 {start|restart|stop|status}" >&2; exit 2 ;;
esac