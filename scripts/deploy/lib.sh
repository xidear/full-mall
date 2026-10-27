#!/usr/bin/env bash
# full-mall 部署脚本公共库。脚本先 set -Eeuo pipefail，再 source 本文件。
# 规则：失败必须暴露；跳过必须打印原因；绝不删除数据卷。

set -Eeuo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEPLOY_ENV_FILE="${DEPLOY_ENV_FILE:-${ROOT_DIR}/.env.deploy}"
DATA_DIR="${DATA_DIR:-/opt/fullmall}"
STATE_FILE="${DATA_DIR}/.deploy-state"
LOG_DIR="${DATA_DIR}/logs"
DRY_RUN="${DRY_RUN:-0}"
ASSUME_YES="${ASSUME_YES:-0}"
RUN_USER="${RUN_USER:-${SUDO_USER:-$(id -un)}}"

log()  { printf '\033[1;34m[%s]\033[0m %s\n' "$(date '+%H:%M:%S')" "$*"; }
ok()   { printf '\033[1;32m[ OK ]\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m[WARN]\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31m[FAIL]\033[0m %s\n' "$*" >&2; exit 1; }

trap 'code=$?; if [[ $code -ne 0 ]]; then echo "[FAIL] 命令失败: line $LINENO: $BASH_COMMAND (exit=$code)" >&2; fi; exit $code' ERR

run_root() {
  if [[ "$(id -u)" -eq 0 ]]; then
    "$@"
  elif command -v sudo >/dev/null 2>&1; then
    sudo "$@"
  else
    die "需要 root 权限执行: $*（请安装 sudo 或用 root 运行）"
  fi
}

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || die "缺少命令: $1"
}

detect_os() {
  [[ -r /etc/os-release ]] || die "无法读取 /etc/os-release，未知发行版"
  # shellcheck disable=SC1091
  . /etc/os-release
  OS_ID="${ID:-unknown}"
  OS_LIKE="${ID_LIKE:-}"
  OS_VERSION="${VERSION_ID:-unknown}"
  OS_NAME="${PRETTY_NAME:-$OS_ID}"
  case "$OS_ID $OS_LIKE" in
    *debian*|*ubuntu*) OS_FAMILY=debian ;;
    *rhel*|*fedora*|*centos*|*rocky*|*almalinux*) OS_FAMILY=rhel ;;
    *alpine*) OS_FAMILY=alpine ;;
    *) OS_FAMILY=unknown ;;
  esac
}

docker_ok() {
  command -v docker >/dev/null 2>&1 && docker info >/dev/null 2>&1
}

compose_ok() {
  command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1
}

state_get() {
  [[ -f "$STATE_FILE" ]] || { printf ''; return 0; }
  awk -F= -v key="$1" '$1==key { $1=""; sub(/^=/, "", $0); print $0; exit }' "$STATE_FILE"
}

state_set() {
  local key="$1" value="$2" tmp
  mkdir -p "$(dirname "$STATE_FILE")"
  tmp="$(mktemp)"
  if [[ -f "$STATE_FILE" ]]; then
    awk -F= -v key="$key" '$1!=key' "$STATE_FILE" > "$tmp"
  fi
  printf '%s=%s\n' "$key" "$value" >> "$tmp"
  mv "$tmp" "$STATE_FILE"
  chmod 600 "$STATE_FILE"
}

# state_skip <step> <fingerprint>：当前状态与“步骤成功后的结果”一致则跳过
state_skip() {
  local step="$1" fingerprint="$2" current
  current="$(state_get "$step")"
  if [[ -n "$current" && "$current" == "$fingerprint" ]]; then
    ok "跳过 $step（状态已一致: $fingerprint）"
    return 0
  fi
  return 1
}

state_done() {
  if [[ "$DRY_RUN" == "1" ]]; then
    ok "DRY 完成 $1（$2，不写状态）"
    return 0
  fi
  state_set "$1" "$2"
  ok "完成 $1（$2）"
}

load_deploy_env() {
  if [[ ! -f "$DEPLOY_ENV_FILE" ]]; then
    [[ -f "$ROOT_DIR/deploy/deploy.env.example" ]] || die "缺少 deploy/deploy.env.example"
    cp "$ROOT_DIR/deploy/deploy.env.example" "$DEPLOY_ENV_FILE"
    warn "已从模板创建 $DEPLOY_ENV_FILE，请按需修改后重新运行"
  fi
  # shellcheck disable=SC1090
  set -a; . "$DEPLOY_ENV_FILE"; set +a
  if [[ -n "${DATA_DIR_OVERRIDE:-}" ]]; then
    DATA_DIR="$DATA_DIR_OVERRIDE"
  fi
  DATA_DIR="${DATA_DIR:-/opt/fullmall}"
  STATE_FILE="${DATA_DIR}/.deploy-state"
  LOG_DIR="${DATA_DIR}/logs"
  mkdir -p "$DATA_DIR" "$LOG_DIR" || die "无法创建数据目录: $DATA_DIR（请检查权限或用 --data-dir 指定）"
  RUN_USER="${RUN_USER:-${SUDO_USER:-$(id -un)}}"
}

compose_base_files() {
  local files=(-f "$ROOT_DIR/compose.yaml")
  case "${DEPLOY_ARTIFACT_MODE:-mounted}" in
    mounted) [[ -f "$ROOT_DIR/compose.mount.yaml" ]] && files+=(-f "$ROOT_DIR/compose.mount.yaml") ;;
    image)   [[ -f "$ROOT_DIR/compose.image.yaml" ]] && files+=(-f "$ROOT_DIR/compose.image.yaml") ;;
    dev)     ;;
    *) die "DEPLOY_ARTIFACT_MODE 只能是 mounted/image/dev，实际=${DEPLOY_ARTIFACT_MODE}" ;;
  esac
  printf '%s\n' "${files[@]}"
}

compose() {
  local files
  mapfile -t files < <(compose_base_files)
  docker compose --env-file "$ROOT_DIR/.env" "${files[@]}" "$@"
}

ops_compose() {
  docker compose --env-file "$ROOT_DIR/.env.ops" -f "$ROOT_DIR/compose.ops.yaml" "$@"
}

confirm() {
  local prompt="$1"
  if [[ "$ASSUME_YES" == "1" ]]; then return 0; fi
  [[ -t 0 ]] || die "非交互环境需要 --yes 才能继续: $prompt"
  read -r -p "$prompt [y/N] " answer
  [[ "$answer" =~ ^[Yy]$ ]] || die "用户取消"
}

wait_http() {
  local url="$1" seconds="${2:-120}" name="${3:-$1}"
  local deadline=$((SECONDS + seconds))
  while (( SECONDS < deadline )); do
    if command -v curl >/dev/null 2>&1; then
      curl -fsS --max-time 3 "$url" >/dev/null 2>&1 && { ok "$name 就绪: $url"; return 0; }
    else
      python3 - "$url" <<'PY' >/dev/null 2>&1 && { ok "$name 就绪: $url"; return 0; }
import sys, urllib.request
urllib.request.urlopen(sys.argv[1], timeout=3).read()
PY
    fi
    sleep 2
  done
  die "$name 在 ${seconds}s 内未就绪: $url"
}

wait_pg() {
  local host="$1" port="${2:-5432}" user="${3:-fullmall}" db="${4:-fullmall}" seconds="${5:-120}"
  local deadline=$((SECONDS + seconds))
  while (( SECONDS < deadline )); do
    if command -v pg_isready >/dev/null 2>&1 && pg_isready -h "$host" -p "$port" -U "$user" -d "$db" >/dev/null 2>&1; then
      ok "PostgreSQL 就绪: $host:$port/$db"; return 0
    fi
    if docker run --rm postgres:17-alpine pg_isready -h "$host" -p "$port" -U "$user" -d "$db" >/dev/null 2>&1; then
      ok "PostgreSQL 就绪: $host:$port/$db"; return 0
    fi
    sleep 2
  done
  die "PostgreSQL 在 ${seconds}s 内未就绪: $host:$port/$db"
}

set_env_key() {
  local file="$1" key="$2" value="$3" force="${4:-0}"
  [[ -f "$file" ]] || : > "$file"
  local escaped
  escaped="$(printf '%s' "$value" | sed -e 's/[\\&|]/\\&/g')"
  if grep -q "^${key}=" "$file"; then
    if [[ "$force" == "1" ]]; then
      sed -i "s|^${key}=.*|${key}=${escaped}|" "$file"
    fi
  else
    printf '%s=%s\n' "$key" "$value" >> "$file"
  fi
}

print_banner() {
  cat <<'BANNER'
============================================================
 Full Mall 部署脚本
 规则：不删除数据卷；失败即停；已完成的步骤按状态跳过
============================================================
BANNER
}
