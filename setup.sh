#!/usr/bin/env bash
# setup.sh：在 init.sh 之上生成配置、构建/拉取产物、初始化数据库并启动。
# 不覆盖生产数据：只创建缺失对象；不执行 down -v；不删除任何 volume。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/deploy/lib.sh
. "${SCRIPT_DIR}/scripts/deploy/lib.sh"

print_banner
if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  echo "用法: bash setup.sh [--yes] [--skip-init] [--no-build] [--force-env] [--db-mode local|external|primary-replica] [--domain host:port]"
  exit 0
fi
load_deploy_env
detect_os

SKIP_INIT=0
NO_BUILD=0
FORCE_ENV=0
REMOTE_CHILD=0
DB_MODE_OVERRIDE=""
DOMAIN_OVERRIDE=""

usage() {
  cat <<'USAGE'
用法: sudo bash setup.sh [选项]
  --yes               非交互
  --skip-init         跳过 init.sh（Docker 已就绪）
  --no-build          跳过源码构建（使用已有 jar/dist）
  --force-env         覆盖 .env 中的域名/URL 等部署键（仍不碰数据库）
  --db-mode MODE      local|external|primary-replica
  --domain host:port  覆盖 SITE_DOMAIN
  --remote-child      由 remote.sh 调用（不递归分发）
  -h, --help
USAGE
}
while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes|-y) ASSUME_YES=1 ;;
    --skip-init) SKIP_INIT=1 ;;
    --no-build) NO_BUILD=1 ;;
    --force-env) FORCE_ENV=1 ;;
    --db-mode) DB_MODE_OVERRIDE="$2"; shift ;;
    --domain) DOMAIN_OVERRIDE="$2"; shift ;;
    --remote-child) REMOTE_CHILD=1 ;;
    -h|--help) usage; exit 0 ;;
    *) die "未知参数: $1（--help 查看用法）" ;;
  esac
  shift
done

[[ -n "$DB_MODE_OVERRIDE" ]] && DB_MODE="$DB_MODE_OVERRIDE"
[[ -n "$DOMAIN_OVERRIDE" ]] && SITE_DOMAIN="$DOMAIN_OVERRIDE"
DB_MODE="${DB_MODE:-local}"
SITE_DOMAIN="${SITE_DOMAIN:-company.binnarui.com:443}"
CODE_DIR="${CODE_DIR:-${ROOT_DIR}}"
[[ "$CODE_DIR" = /* ]] || die "CODE_DIR 必须是绝对路径: $CODE_DIR"
[[ -f "$CODE_DIR/compose.yaml" ]] || die "找不到 $CODE_DIR/compose.yaml"
[[ "$SITE_DOMAIN" =~ ^[A-Za-z0-9.-]+(:[0-9]{1,5})?$ ]] || die "SITE_DOMAIN 格式错误: $SITE_DOMAIN"

if [[ "$SKIP_INIT" != "1" ]]; then
  bash "${CODE_DIR}/init.sh" --yes
fi
docker_ok || die "Docker 不可用；请先运行 sudo bash init.sh"
compose_ok || die "Docker Compose 插件不可用"

# ---------- .env ----------
cd "$CODE_DIR"
[[ -f .env ]] || { cp .env.example .env; warn "已创建 .env"; }

site_host="${SITE_DOMAIN%%:*}"
site_port="${SITE_DOMAIN##*:}"
[[ "$site_port" == "$SITE_DOMAIN" ]] && site_port=443
PUBLIC_SCHEME="${PUBLIC_SCHEME:-https}"
PUBLIC_URL="${PUBLIC_SCHEME}://${SITE_DOMAIN}"

set_public_key() {
  local key="$1" value="$2" current
  current="$(grep -E "^${key}=" .env | head -n1 | cut -d= -f2- || true)"
  if [[ "$FORCE_ENV" == "1" || -z "$current" || "$current" == *localhost* || "$current" == "3000" || "$current" == "8080" || "$current" == "5173" || "$current" == *:3000* || "$current" == *:8080* || "$current" == *:5173* ]]; then
    set_env_key .env "$key" "$value" 1
  fi
}

set_public_key APP_BASE_URL "$PUBLIC_URL"
set_public_key API_PUBLIC_URL "$PUBLIC_URL"
set_public_key SITE_DOMAIN "$SITE_DOMAIN"
set_public_key NUXT_PUBLIC_API_BASE "$PUBLIC_URL"
set_public_key OPS_AGENTS "local=http://host.docker.internal:${OPS_AGENT_PORT:-41287}"
set_public_key OPS_TOKEN "${OPS_TOKEN:-change-me-ops-token}"
set_public_key SSL_HELPER_URL "http://host.docker.internal:${OPS_AGENT_PORT:-41287}"
set_public_key API_PORT "${API_PORT:-41280}"
set_public_key ADMIN_PORT "${ADMIN_PORT:-41281}"
set_public_key STOREFRONT_PORT "${STOREFRONT_PORT:-41285}"
set_public_key ADMIN_URL "http://${SITE_DOMAIN%%:*}:${ADMIN_PORT:-41281}"

if [[ "$DB_MODE" == "external" ]]; then
  set_env_key .env SPRING_DATASOURCE_URL "jdbc:postgresql://${DB_PRIMARY_HOST}:${DB_PRIMARY_PORT:-5432}/${POSTGRES_DB:-fullmall}" 1
  set_env_key .env SPRING_DATASOURCE_USERNAME "${POSTGRES_USER:-fullmall}" 1
  set_env_key .env SPRING_DATASOURCE_PASSWORD "${POSTGRES_PASSWORD:-change-me-local}" 1
elif [[ "$DB_MODE" == "primary-replica" ]]; then
  set_env_key .env SPRING_DATASOURCE_URL "jdbc:postgresql://postgres-primary:5432/${POSTGRES_DB:-fullmall}" 1
  set_env_key .env SPRING_DATASOURCE_USERNAME "${POSTGRES_USER:-fullmall}" 1
  set_env_key .env SPRING_DATASOURCE_PASSWORD "${POSTGRES_PASSWORD:-change-me-local}" 1
fi

# ---------- .env.ops ----------
[[ -f .env.ops ]] || { cp .env.ops.example .env.ops; warn "已创建 .env.ops"; }
set_env_key .env.ops SSL_DOMAIN "$site_host" 1
set_env_key .env.ops SSL_EMAIL "${SSL_EMAIL:-}" 1
set_env_key .env.ops SSL_CA "${SSL_CA:-letsencrypt}" 1
ssl_challenge="${SSL_CHALLENGE:-http}"
if [[ "$ssl_challenge" == "dns" && ( -z "${DP_ID:-}" || -z "${DP_KEY:-}" ) ]]; then
  warn "SSL_CHALLENGE=dns 但 DP_ID/DP_KEY 为空，回退 http（需要外网 80）"
  ssl_challenge=http
fi
set_env_key .env.ops SSL_CHALLENGE "$ssl_challenge" 1
set_env_key .env.ops DP_ID "${DP_ID:-}" 1
set_env_key .env.ops DP_KEY "${DP_KEY:-}" 1
set_env_key .env.ops PUBLIC_HTTPS_PORT "$site_port" 1
set_env_key .env.ops OPS_AGENT_PORT "${OPS_AGENT_PORT:-41287}" 1
set_env_key .env.ops OPS_TOKEN "${OPS_TOKEN:-change-me-ops-token}" 1
set_env_key .env.ops EDGE_UPSTREAM "http://127.0.0.1:${STOREFRONT_PORT:-41285}" 1
set_env_key .env.ops EDGE_API_UPSTREAM "http://127.0.0.1:${API_PORT:-41280}" 1

# ---------- 构建产物 ----------
build_backend() {
  local jar="server/target/fullmall-server-0.1.0-SNAPSHOT.jar"
  if [[ ! -f "$jar" ]]; then
    log "后端 jar 不存在，开始构建 ..."
  elif find server/src server/pom.xml -newer "$jar" -print -quit | grep -q .; then
    log "后端源码比 jar 新，重新构建 ..."
  else
    ok "后端 jar 已是最新，跳过构建"
    return 0
  fi
  command -v mvn >/dev/null 2>&1 || die "需要 Maven；或设置 DEPLOY_ARTIFACT_MODE=image 使用镜像"
  ( cd server && mvn -B -q -DskipTests package )
  ok "后端构建完成: $jar"
}

build_frontend() {
  local fingerprint
  fingerprint="$(find web/apps web/packages web/package.json -type f -not -path '*/node_modules/*' -not -path '*/dist/*' -not -path '*/.output/*' -not -path '*/.nuxt/*' -print0 2>/dev/null | sort -z | xargs -0 sha256sum 2>/dev/null | sha256sum | awk '{print $1}')"
  if state_skip frontend_build "$fingerprint"; then
    return 0
  fi
  command -v npm >/dev/null 2>&1 || die "需要 Node.js/npm；或设置 DEPLOY_ARTIFACT_MODE=image"
  if [[ ! -d web/node_modules ]]; then
    ( cd web && npm ci )
  fi
  ( cd web && npm run build --workspaces --if-present )
  state_done frontend_build "$fingerprint"
}

case "${DEPLOY_ARTIFACT_MODE:-mounted}" in
  mounted)
    if [[ "$NO_BUILD" != "1" ]]; then
      build_backend
      build_frontend
    else
      log "NO_BUILD=1，跳过源码构建"
    fi
    ;;
  image)
    log "镜像模式：拉取应用镜像 ..."
    docker compose --env-file .env -f compose.yaml -f compose.image.yaml pull
    if [[ -n "${DOCKER_REGISTRY:-}" ]]; then
      docker compose --env-file .env -f compose.yaml -f compose.image.yaml pull
    fi
    ;;
  dev) log "dev 模式：使用源码挂载/本地进程" ;;
  *) die "DEPLOY_ARTIFACT_MODE 非法: $DEPLOY_ARTIFACT_MODE" ;;
esac

# ---------- 数据库 ----------
case "$DB_MODE" in
  local)
    log "启动本机数据库与基础设施（不清空卷）..."
    compose --profile core up -d postgres valkey rabbitmq opensearch seaweedfs mailpit
    wait_pg 127.0.0.1 "${POSTGRES_PORT:-5432}" "${POSTGRES_USER:-fullmall}" "${POSTGRES_DB:-fullmall}" 180
    ;;
  external)
    log "使用外部主库: $DB_PRIMARY_HOST:$DB_PRIMARY_PORT"
    wait_pg "${DB_PRIMARY_HOST}" "${DB_PRIMARY_PORT:-5432}" "${POSTGRES_USER:-fullmall}" "${POSTGRES_DB:-fullmall}" 120
    ;;
  primary-replica)
    log "启动主从数据库（Bitnami PostgreSQL replication）..."
    docker compose --env-file .env -f compose.db-ha.yaml up -d
    wait_pg 127.0.0.1 "${DB_HA_PRIMARY_PORT:-5432}" "${POSTGRES_USER:-fullmall}" "${POSTGRES_DB:-fullmall}" 180
    ok "主库就绪；从库由 compose 健康检查保证"
    ;;
  *) die "DB_MODE 只能是 local|external|primary-replica，实际=$DB_MODE" ;;
esac

# ---------- 启动应用 ----------
bash "${CODE_DIR}/start.sh" --no-build
if [[ "${WITH_OPS:-1}" == "1" ]]; then
  log "启动 ops/SSL 辅助容器 ..."
  if [[ "${WITH_EDGE:-1}" == "1" ]]; then
    ops_compose --profile edge up -d --build
  else
    ops_compose up -d --build ops
  fi
fi

# ---------- 写入 site_domain（只 upsert，不删数据） ----------
wait_http "http://127.0.0.1:${API_PORT:-41280}/actuator/health" 180 "API"
log "写入 site_domain=$SITE_DOMAIN ..."
if [[ "$DB_MODE" == "local" ]]; then
  compose exec -T postgres psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER:-fullmall}" -d "${POSTGRES_DB:-fullmall}" \
    -c "INSERT INTO system_setting (setting_key, setting_value, updated_at) VALUES ('site_domain', '$SITE_DOMAIN', now()) ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = now();"
elif [[ "$DB_MODE" == "primary-replica" ]]; then
  docker compose --env-file .env -f compose.db-ha.yaml exec -T postgres-primary psql -v ON_ERROR_STOP=1 -U "${POSTGRES_USER:-fullmall}" -d "${POSTGRES_DB:-fullmall}" \
    -c "INSERT INTO system_setting (setting_key, setting_value, updated_at) VALUES ('site_domain', '$SITE_DOMAIN', now()) ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = now();"
else
  if command -v psql >/dev/null 2>&1; then
    PGPASSWORD="${POSTGRES_PASSWORD:-change-me-local}" psql -h "${DB_PRIMARY_HOST}" -p "${DB_PRIMARY_PORT:-5432}" -U "${POSTGRES_USER:-fullmall}" -d "${POSTGRES_DB:-fullmall}" \
      -c "INSERT INTO system_setting (setting_key, setting_value, updated_at) VALUES ('site_domain', '$SITE_DOMAIN', now()) ON CONFLICT (setting_key) DO UPDATE SET setting_value = EXCLUDED.setting_value, updated_at = now();"
  else
    warn "外部库且本机无 psql；请手工执行：INSERT INTO system_setting ... 'site_domain'='$SITE_DOMAIN'"
  fi
fi

# ---------- 多机分发 ----------
if [[ "${DEPLOY_MODE:-single}" == "distributed" && "$REMOTE_CHILD" != "1" ]]; then
  log "DEPLOY_MODE=distributed，分发到 deploy/hosts.conf ..."
  bash "${CODE_DIR}/scripts/deploy/remote.sh" setup
fi

ok "部署完成"
cat <<SUMMARY
商城:   $PUBLIC_URL
后台:   http://${SITE_DOMAIN%%:*}:${ADMIN_PORT:-41281}
API:    http://${SITE_DOMAIN%%:*}:${API_PORT:-41280}
运维:   http://${SITE_DOMAIN%%:*}:${ADMIN_PORT:-41281}/ops
证书:   $PUBLIC_URL（首次申请需要 80 端口可达）
SUMMARY
