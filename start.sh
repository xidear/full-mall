#!/usr/bin/env bash
# start.sh：启动 full-mall（基础设施 + 应用 + 可选 ops/edge）。
# 不构建、不删除数据；需要构建请先运行 setup.sh。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/deploy/lib.sh
. "${SCRIPT_DIR}/scripts/deploy/lib.sh"

print_banner
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  echo "用法: bash start.sh [--build] [--with-tools] [--no-ops] [--no-edge] [--all]"
  exit 0
fi
load_deploy_env
detect_os
docker_ok || die "Docker 不可用；请先运行 sudo bash init.sh"
compose_ok || die "Docker Compose 插件不可用"

WITH_TOOLS_RUN="${WITH_TOOLS:-0}"
WITH_OPS_RUN="${WITH_OPS:-1}"
WITH_EDGE_RUN="${WITH_EDGE:-1}"
BUILD_FLAG=()
ALL_PROFILES=0
while [[ $# -gt 0 ]]; do
  case "$1" in
    --no-build) ;;
    --build) BUILD_FLAG=(--build) ;;
    --with-tools) WITH_TOOLS_RUN=1 ;;
    --no-ops) WITH_OPS_RUN=0 ;;
    --no-edge) WITH_EDGE_RUN=0 ;;
    --all) ALL_PROFILES=1 ;;
    -h|--help) echo "用法: bash start.sh [--build] [--with-tools] [--no-ops] [--no-edge] [--all]"; exit 0 ;;
    *) die "未知参数: $1" ;;
  esac
  shift
done

[[ -f "$ROOT_DIR/.env" ]] || die "缺少 .env；请先运行 sudo bash setup.sh"
log "启动 core + app ..."
SCALE_ARGS=()
if [[ "${DB_MODE:-local}" == "primary-replica" ]]; then
  SCALE_ARGS=(--scale postgres=0)
fi
compose --profile core --profile app "${SCALE_ARGS[@]}" "${BUILD_FLAG[@]}" up -d

if [[ "$WITH_TOOLS_RUN" == "1" || "$ALL_PROFILES" == "1" ]]; then
  log "启动 tools profile ..."
  compose --profile tools up -d
fi
if [[ "$ALL_PROFILES" == "1" ]]; then
  log "启动 obs + ai profile ..."
  compose --profile obs --profile ai up -d
fi

if [[ "$WITH_OPS_RUN" == "1" ]]; then
  [[ -f "$ROOT_DIR/.env.ops" ]] || { cp "$ROOT_DIR/.env.ops.example" "$ROOT_DIR/.env.ops"; warn "已创建 .env.ops"; }
  if [[ "$WITH_EDGE_RUN" == "1" ]]; then
    log "启动 ops + edge（80/443）..."
    ops_compose --profile edge up -d
  else
    log "启动 ops（不启动 edge）..."
    ops_compose up -d ops
  fi
fi

wait_http "http://127.0.0.1:${API_PORT:-41280}/actuator/health" 180 "API"
site_host="${SITE_DOMAIN%%:*}"
cat <<SUMMARY
已启动：
  API:      http://${site_host}:${API_PORT:-41280}
  商城:     ${PUBLIC_SCHEME:-https}://${SITE_DOMAIN}
  后台:     http://${site_host}:${ADMIN_PORT:-41281}
  运维:     http://${site_host}:${ADMIN_PORT:-41281}/ops
SUMMARY
