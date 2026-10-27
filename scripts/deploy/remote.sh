#!/usr/bin/env bash
# 分布式部署：按 deploy/hosts.conf 逐台执行 setup/start/stop。
# 前置：各主机已安装 Docker（可先 ssh 执行 init.sh）、已 clone 本仓库、SSH key 免密。
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=scripts/deploy/lib.sh
. "${SCRIPT_DIR}/../deploy/lib.sh"

ACTION="${1:-start}"
HOSTS_FILE="${HOSTS_FILE:-${ROOT_DIR}/deploy/hosts.conf}"
DRY_RUN="${DRY_RUN:-0}"
[[ -f "$HOSTS_FILE" ]] || die "缺少主机清单: $HOSTS_FILE（可从 deploy/hosts.conf.example 复制）"
require_cmd ssh
require_cmd scp

run_remote() {
  local target="$1" command="$2"
  if [[ "$DRY_RUN" == "1" ]]; then
    log "DRY ssh $target: $command"
  else
    ssh -o BatchMode=yes -o ConnectTimeout=10 "$target" "$command"
  fi
}

copy_file() {
  local target="$1" file="$2" remote_dir="$3"
  if [[ "$DRY_RUN" == "1" ]]; then
    log "DRY scp $file $target:$remote_dir/"
  else
    scp -q "$file" "$target:$remote_dir/"
  fi
}

while IFS='|' read -r name target roles code_dir data_dir; do
  [[ -z "${name// }" || "$name" == \#* ]] && continue
  [[ -n "$target" && "$target" != "local" && "$target" != "-" ]] || { log "跳过本地主机 $name"; continue; }
  [[ "$code_dir" = /* ]] || die "$name 的代码目录必须是绝对路径: $code_dir"
  log "[$name] $target roles=$roles action=$ACTION"
  run_remote "$target" "test -f '$code_dir/compose.yaml'"
  copy_file "$target" "$ROOT_DIR/.env" "$code_dir"
  if [[ -f "$ROOT_DIR/.env.ops" ]]; then copy_file "$target" "$ROOT_DIR/.env.ops" "$code_dir"; fi
  case "$ACTION" in
    setup)
      run_remote "$target" "cd '$code_dir' && sudo bash setup.sh --yes --skip-init --no-build --remote-child"
      ;;
    start)
      if [[ "$roles" == *edge* || "$roles" == *ops* ]]; then
        run_remote "$target" "cd '$code_dir' && WITH_OPS=1 WITH_EDGE=$([[ "$roles" == *edge* ]] && echo 1 || echo 0) bash start.sh --no-build"
      elif [[ "$roles" == *app* ]]; then
        run_remote "$target" "cd '$code_dir' && WITH_OPS=0 WITH_EDGE=0 bash start.sh --no-build"
      else
        run_remote "$target" "cd '$code_dir' && WITH_OPS=0 WITH_EDGE=0 bash start.sh --no-build"
      fi
      ;;
    stop)
      run_remote "$target" "cd '$code_dir' && bash stop.sh --all"
      ;;
    *) die "未知 action: $ACTION（setup|start|stop）" ;;
  esac
done < "$HOSTS_FILE"

ok "远程 $ACTION 完成"
