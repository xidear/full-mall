#!/usr/bin/env bash
# init.sh：初始化宿主机基础能力（Docker、docker 组、镜像加速、目录、镜像）。
# 幂等：每一步完成后把“结果指纹”写入 ${DATA_DIR}/.deploy-state，下次状态一致直接跳过。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/deploy/lib.sh
. "${SCRIPT_DIR}/scripts/deploy/lib.sh"

print_banner

usage() {
  cat <<'USAGE'
用法: sudo bash init.sh [选项]
  --yes                 非交互确认
  --dry-run             只打印将执行的命令
  --region cn|global    指定区域（默认 auto：按公网 IP 判断）
  --mirror URL          Docker 镜像加速地址
  --code-dir PATH       代码目录（绝对路径）
  --data-dir PATH       数据目录（绝对路径）
  --no-pull             不拉取基础镜像
  -h, --help
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --yes|-y) ASSUME_YES=1 ;;
    --dry-run) DRY_RUN=1 ;;
    --region) REGION="$2"; shift ;;
    --mirror) DOCKER_MIRROR="$2"; shift ;;
    --code-dir) CODE_DIR="$2"; shift ;;
    --data-dir) DATA_DIR_OVERRIDE="$2"; shift ;;
    --no-pull) INIT_PULL_IMAGES=0 ;;
    -h|--help) usage; exit 0 ;;
    *) die "未知参数: $1（--help 查看用法）" ;;
  esac
  shift
done

load_deploy_env
detect_os

CODE_DIR="${CODE_DIR:-${ROOT_DIR}}"
[[ "$CODE_DIR" = /* ]] || die "CODE_DIR 必须是绝对路径，实际=$CODE_DIR"
[[ -f "$CODE_DIR/compose.yaml" ]] || die "CODE_DIR 下找不到 compose.yaml: $CODE_DIR"
[[ -f "$CODE_DIR/.env" ]] || { cp "$CODE_DIR/.env.example" "$CODE_DIR/.env"; warn "已从 .env.example 创建 $CODE_DIR/.env"; }

log "系统: $OS_NAME ($OS_FAMILY/$OS_VERSION)"
log "代码目录: $CODE_DIR"
log "数据目录: $DATA_DIR"
log "运行用户: $RUN_USER"

run_do() {
  if [[ "$DRY_RUN" == "1" ]]; then
    log "DRY: $*"
  else
    "$@"
  fi
}
run_root_do() {
  if [[ "$DRY_RUN" == "1" ]]; then
    log "DRY(root): $*"
  else
    run_root "$@"
  fi
}

install_docker_debian() {
  run_root_do apt-get update
  run_root_do apt-get install -y ca-certificates curl gnupg python3
  run_root_do install -m 0755 -d /etc/apt/keyrings
  if [[ ! -s /etc/apt/keyrings/docker.asc ]]; then
    curl -fsSL "https://download.docker.com/linux/${OS_ID}/gpg" -o /tmp/docker.asc
    run_root_do install -m 0644 /tmp/docker.asc /etc/apt/keyrings/docker.asc
  fi
  local arch codename
  arch="$(dpkg --print-architecture)"
  codename="${VERSION_CODENAME:-}"
  [[ -n "$codename" ]] || die "无法确定 Debian/Ubuntu 代号（VERSION_CODENAME 为空）"
  echo "deb [arch=$arch signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/${OS_ID} $codename stable" > /tmp/docker.list
  run_root_do install -m 0644 /tmp/docker.list /etc/apt/sources.list.d/docker.list
  run_root_do apt-get update
  run_root_do apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
}

install_docker_rhel() {
  run_root_do dnf install -y dnf-plugins-core curl python3
  run_root_do dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
  run_root_do dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
}

install_docker_alpine() {
  run_root_do apk add --no-cache docker docker-cli-compose
  run_root_do rc-update add docker default
  run_root_do service docker start
}

if docker_ok && compose_ok; then
  version="$(docker --version)"
  state_skip docker_installed "$version" || state_done docker_installed "$version"
else
  log "安装 Docker Engine + Compose 插件 ..."
  case "$OS_FAMILY" in
    debian) install_docker_debian ;;
    rhel) install_docker_rhel ;;
    alpine) install_docker_alpine ;;
    *)
      if [[ "${ALLOW_GET_DOCKER:-0}" == "1" ]]; then
        warn "未知发行版，使用官方 get.docker.com 脚本（ALLOW_GET_DOCKER=1）"
        curl -fsSL https://get.docker.com | run_root_do sh
      else
        die "不支持的发行版 $OS_NAME；请安装 Docker 或设置 ALLOW_GET_DOCKER=1"
      fi
      ;;
  esac
  if [[ "$DRY_RUN" != "1" ]]; then
    if command -v systemctl >/dev/null 2>&1; then
      run_root systemctl enable --now docker
    elif command -v service >/dev/null 2>&1; then
      run_root service docker start
    fi
    docker_ok || die "Docker 安装后仍不可用，请检查 systemctl status docker"
    compose_ok || die "Docker Compose 插件不可用，请安装 docker-compose-plugin"
  fi
  state_done docker_installed "$(docker --version 2>/dev/null || echo dry-run)"
fi

# docker 组
if getent group docker >/dev/null 2>&1; then
  state_skip docker_group "group-exists" || state_done docker_group "group-exists"
else
  run_root_do groupadd docker
  state_done docker_group "group-exists"
fi
if id "$RUN_USER" >/dev/null 2>&1; then
  if id -nG "$RUN_USER" | tr ' ' '\n' | grep -qx docker; then
    state_skip docker_user "user-in-docker-group" || state_done docker_user "user-in-docker-group"
  else
    run_root_do usermod -aG docker "$RUN_USER"
    NEED_RELOGIN=1
    state_done docker_user "user-in-docker-group"
  fi
else
  warn "运行用户不存在: $RUN_USER（跳过 docker 组；请用 --env RUN_USER=... 指定）"
fi

# 区域与镜像加速
REGION="${REGION:-auto}"
if [[ "$REGION" == "auto" ]]; then
  country="$(curl -fsS --max-time 5 https://ipinfo.io/country 2>/dev/null || true)"
  if [[ -z "$country" ]]; then
    warn "无法通过 ipinfo.io 判断区域，保持现有 Docker 镜像配置（可显式设置 REGION=cn|global）"
  elif [[ "$country" == "CN" ]]; then
    REGION=cn
  else
    REGION=global
  fi
fi
if [[ "$REGION" == "cn" && -n "${DOCKER_MIRROR:-}" ]]; then
  daemon_json="/etc/docker/daemon.json"
  tmp_json="$(mktemp)"
  if [[ -f "$daemon_json" ]]; then
    cp "$daemon_json" "$tmp_json"
  else
    echo '{}' > "$tmp_json"
  fi
  python3 - "$tmp_json" "$DOCKER_MIRROR" <<'PY'
import json, sys
path, mirror = sys.argv[1], sys.argv[2]
with open(path, encoding='utf-8') as fh:
    data = json.load(fh)
mirrors = data.setdefault('registry-mirrors', [])
if mirror not in mirrors:
    mirrors.insert(0, mirror)
with open(path, 'w', encoding='utf-8') as fh:
    json.dump(data, fh, ensure_ascii=False, indent=2)
    fh.write('\n')
PY
  if [[ -f "$daemon_json" ]] && cmp -s "$daemon_json" "$tmp_json"; then
    state_skip docker_mirror "$DOCKER_MIRROR"
  else
    run_root_do install -m 0644 -D "$tmp_json" "$daemon_json"
    if [[ "$DRY_RUN" != "1" ]]; then
      run_root systemctl restart docker 2>/dev/null || run_root service docker restart
    fi
    state_done docker_mirror "$DOCKER_MIRROR"
  fi
  rm -f "$tmp_json"
else
  log "区域=$REGION，不配置 Docker 镜像加速（如需请设置 REGION=cn + DOCKER_MIRROR）"
  state_set docker_mirror "$REGION"
fi

# 代码目录权限
if [[ "${FIX_CODE_OWNER:-1}" == "1" && "$(id -u)" -eq 0 ]] && id "$RUN_USER" >/dev/null 2>&1; then
  owner="$(stat -c %U "$CODE_DIR")"
  if [[ "$owner" != "$RUN_USER" ]]; then
    log "代码目录属主为 $owner，改为 $RUN_USER ..."
    run_root_do chown -R "$RUN_USER:$RUN_USER" "$CODE_DIR"
  else
    state_skip code_owner "$owner" || state_done code_owner "$owner"
  fi
else
  log "跳过代码目录属主修改（非 root 或 FIX_CODE_OWNER=0）"
fi

# 数据目录（只创建，不删除）
for dir in postgres valkey rabbitmq opensearch seaweedfs certs acme logs; do
  run_do mkdir -p "$DATA_DIR/$dir"
done
if [[ "$DRY_RUN" != "1" && "$(id -u)" -eq 0 ]] && id "$RUN_USER" >/dev/null 2>&1; then
  chown -R "$RUN_USER:$RUN_USER" "$DATA_DIR"
fi
state_done data_dirs "$DATA_DIR"

# 私有仓库登录
if [[ -n "${DOCKER_REGISTRY:-}" ]]; then
  if [[ -z "${DOCKER_USERNAME:-}" ]]; then
    die "DOCKER_REGISTRY 已设置但 DOCKER_USERNAME 为空"
  fi
  if [[ -z "${DOCKER_PASSWORD:-}" ]]; then
    [[ -t 0 ]] || die "需要 DOCKER_PASSWORD 或交互终端输入"
    read -rsp "Docker 仓库 $DOCKER_REGISTRY 的密码: " DOCKER_PASSWORD
    echo
  fi
  if [[ "$DRY_RUN" == "1" ]]; then
    log "DRY: docker login $DOCKER_REGISTRY -u $DOCKER_USERNAME --password-stdin"
  elif docker info >/dev/null 2>&1; then
    printf '%s' "$DOCKER_PASSWORD" | docker login "$DOCKER_REGISTRY" -u "$DOCKER_USERNAME" --password-stdin
    state_done docker_login "$DOCKER_REGISTRY:$DOCKER_USERNAME"
  else
    # 刚加入 docker 组的当前 shell 可能还没生效；用 root 的 docker 配置完成登录
    printf '%s' "$DOCKER_PASSWORD" | run_root docker login "$DOCKER_REGISTRY" -u "$DOCKER_USERNAME" --password-stdin
    state_done docker_login "$DOCKER_REGISTRY:$DOCKER_USERNAME"
  fi
fi

# 拉取基础镜像（按 compose 配置的镜像清单做指纹）
if [[ "${INIT_PULL_IMAGES:-1}" == "1" ]]; then
  images="$(cd "$CODE_DIR" && docker compose --env-file .env -f compose.yaml --profile core --profile app --profile tools config --images 2>/dev/null | sort -u || true)"
  if [[ -z "$images" ]]; then
    warn "未能从 compose 读取镜像清单，跳过拉取"
  else
    fingerprint="$(printf '%s\n' "$images" | sha256sum | awk '{print $1}')"
    if state_skip docker_images "$fingerprint"; then
      :
    else
      if [[ "$DRY_RUN" == "1" ]]; then
        log "DRY: docker compose pull（$(printf '%s\n' "$images" | wc -l) 个镜像）"
      else
        (cd "$CODE_DIR" && docker compose --env-file .env -f compose.yaml --profile core --profile app --profile tools pull)
      fi
      state_done docker_images "$fingerprint"
    fi
  fi
else
  log "INIT_PULL_IMAGES=0，跳过镜像拉取"
fi

ok "初始化完成"
echo
log "下一步：sudo bash setup.sh --yes"
if [[ "${NEED_RELOGIN:-0}" == "1" ]]; then
  warn "已将 $RUN_USER 加入 docker 组；当前 shell 需重新登录（或 newgrp docker）才生效。"
fi
