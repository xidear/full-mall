#!/usr/bin/env bash
# 开发阶段的 admin 服务（Vite dev），15052 → 192.168.1.15:41281。
#
# 取舍（2026-10-31 用户确认）：开发阶段以速度为主，41281 跑 dev（源码热更新）；
# 上线前用 scripts/admin-preview.sh 切到构建产物。
#
# 用法：
#   scripts/admin-dev.sh start|stop|restart|status
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_FILE="${TMPDIR:-/tmp}/fullmall-admin-dev.pid"
LOG_FILE="${TMPDIR:-/tmp}/fullmall-admin-dev.log"
PORT=41281
PATTERN="vite --host 0.0.0.0 --port ${PORT}"

probe() {
  python3 - "$PORT" <<'PY'
import socket, sys
s = socket.socket(); s.settimeout(0.4)
sys.exit(0 if s.connect_ex(('127.0.0.1', int(sys.argv[1]))) == 0 else 1)
PY
}

stop_server() {
  local pids
  pids="$(pgrep -f "$PATTERN" || true)"
  if [[ -n "$pids" ]]; then
    # shellcheck disable=SC2086
    kill $pids 2>/dev/null || true
    sleep 1
  fi
  rm -f "$PID_FILE"
}

start() {
  if probe; then
    echo "41281 已被占用（可能是 preview）：先 scripts/admin-preview.sh stop 或确认占用者" >&2
    exit 1
  fi
  (cd "$ROOT/web/apps/admin" && setsid -f npx vite --host 0.0.0.0 --port "$PORT" --strictPort \
      > "$LOG_FILE" 2>&1 < /dev/null)
  for _ in $(seq 1 20); do
    probe && { pgrep -f "$PATTERN" | head -1 > "$PID_FILE" || true;
               echo "admin dev 已启动：http://127.0.0.1:${PORT}（日志 $LOG_FILE）"; return 0; }
    sleep 1
  done
  echo "启动失败，查看日志：$LOG_FILE" >&2
  tail -20 "$LOG_FILE" >&2 || true
  exit 1
}

status() {
  if probe; then
    echo "41281：UP"
    [[ -f "$PID_FILE" ]] && echo "dev pid：$(cat "$PID_FILE")"
  else
    echo "41281：DOWN"; exit 1
  fi
}

case "${1:-status}" in
  start) start ;;
  restart) stop_server; start ;;
  stop) stop_server; echo "已停止 dev" ;;
  status) status ;;
  *) echo "用法: $0 {start|restart|stop|status}" >&2; exit 2 ;;
esac