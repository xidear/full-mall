#!/usr/bin/env bash
# stop.sh：停止容器，保留所有数据卷。绝不会执行 down -v。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/deploy/lib.sh
. "${SCRIPT_DIR}/scripts/deploy/lib.sh"

print_banner
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  echo "用法: bash stop.sh [--all] [--keep-ops] [--host-api]"
  exit 0
fi
load_deploy_env
STOP_ALL=0
KEEP_OPS=0
STOP_HOST_API=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --all) STOP_ALL=1 ;;
    --keep-ops) KEEP_OPS=1 ;;
    --host-api) STOP_HOST_API=1 ;;
    -h|--help) echo "用法: bash stop.sh [--all] [--keep-ops] [--host-api]"; exit 0 ;;
    *) die "未知参数: $1" ;;
  esac
  shift
done

if [[ -f "$ROOT_DIR/.env" ]]; then
  if [[ "$STOP_ALL" == "1" ]]; then
    log "停止 core/app/tools/obs/ai（保留卷）..."
    compose --profile core --profile app --profile tools --profile obs --profile ai down
  else
    log "停止 core/app/tools（保留卷）..."
    compose --profile core --profile app --profile tools down
  fi
else
  warn "缺少 .env，跳过主栈停止"
fi

if [[ "$KEEP_OPS" != "1" && -f "$ROOT_DIR/.env.ops" ]]; then
  log "停止 ops/edge（保留卷）..."
  ops_compose --profile edge down
fi

if [[ "$STOP_HOST_API" == "1" ]]; then
  pids="$(pgrep -f 'java -jar .*fullmall-server-.*\.jar' || true)"
  if [[ -n "$pids" ]]; then
    log "停止宿主 API 进程: $pids"
    # shellcheck disable=SC2086
    kill $pids
  else
    log "未发现宿主 API 进程"
  fi
fi

ok "已停止；数据卷未删除。重新启动: bash start.sh"
