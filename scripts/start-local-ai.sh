#!/usr/bin/env bash
# 启动本机 AI（Ollama）：0.xB 文本模型 + embedding，纳入自有管理。
#
# 用法：
#   bash scripts/start-local-ai.sh              # 联网：缺模型会自动 pull，最后做真实推理自检
#   bash scripts/start-local-ai.sh --offline    # 离线/内网：只启动 + 校验模型已在卷里，缺模型直接失败
#
# 模型权重不在 ollama 镜像内（qwen2.5:0.5b 约 400MB、nomic-embed-text 约 274MB），
# 所以离线部署必须先在有网机器上把模型写进 ollama-data 卷，再整体迁移该卷；
# --offline 的作用就是把"以为配好了、其实没模型"变成启动时的明确失败。
set -euo pipefail

TEXT_MODEL="${TEXT_MODEL:-qwen2.5:0.5b}"
EMB_MODEL="${EMB_MODEL:-nomic-embed-text}"
PORT="${OLLAMA_PORT:-11434}"
SERVICE="${OLLAMA_SERVICE:-ollama}"
OFFLINE=0

for arg in "$@"; do
  case "$arg" in
    --offline) OFFLINE=1 ;;
    -h|--help) sed -n '2,12p' "$0"; exit 0 ;;
    *) echo "[ai] 不认识的参数：$arg" >&2; exit 2 ;;
  esac
done

cd "$(dirname "$0")/.."

echo "[ai] 启动 ollama 容器 ..."
docker compose --profile ai up -d "$SERVICE"

# 用容器内的 ollama 命令判断就绪，不依赖宿主是否装了 curl。
ready=0
for _ in $(seq 1 60); do
  if docker compose --profile ai exec -T "$SERVICE" ollama list >/dev/null 2>&1; then ready=1; break; fi
  sleep 1
done
if [[ "$ready" != "1" ]]; then
  echo "[ai] ollama 60 秒内没有就绪；查看容器日志：docker compose --profile ai logs $SERVICE" >&2
  exit 1
fi

installed() {
  docker compose --profile ai exec -T "$SERVICE" ollama list | awk 'NR>1 {print $1}' | grep -qx "$1"
}

if [[ "$OFFLINE" == "1" ]]; then
  missing=()
  installed "$TEXT_MODEL" || missing+=("$TEXT_MODEL")
  installed "$EMB_MODEL" || missing+=("$EMB_MODEL")
  if (( ${#missing[@]} )); then
    echo "[ai] 离线模式缺少模型：${missing[*]}" >&2
    echo "[ai] 请在有网机器上执行：docker compose --profile ai exec -T $SERVICE ollama pull <模型>，再把 ollama-data 卷迁移过来" >&2
    exit 1
  fi
  echo "[ai] 离线模式：模型已在卷内，跳过拉取"
else
  echo "[ai] 拉取模型 ${TEXT_MODEL} / ${EMB_MODEL}（已存在的会跳过）..."
  docker compose --profile ai exec -T "$SERVICE" ollama pull "$TEXT_MODEL"
  docker compose --profile ai exec -T "$SERVICE" ollama pull "$EMB_MODEL"
fi

echo "[ai] 自检：chat + embedding ..."
python3 - "$PORT" "$TEXT_MODEL" "$EMB_MODEL" <<'PY'
import json, sys, urllib.request
port, text, emb = sys.argv[1], sys.argv[2], sys.argv[3]
def post(path, payload):
    req = urllib.request.Request(f"http://localhost:{port}{path}",
                                 data=json.dumps(payload).encode(),
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.load(r)
chat = post("/v1/chat/completions", {"model": text, "messages": [{"role": "user", "content": "回复：ok"}]})
content = chat["choices"][0]["message"]["content"].strip()
if not content:
    raise SystemExit("chat 返回为空")
vector = post("/v1/embeddings", {"model": emb, "input": "陶瓷马克杯"})["data"][0]["embedding"]
print(f"[ai] chat ok（{content[:20]}），embedding dim={len(vector)}")
PY

echo "[ai] 就绪：http://localhost:${PORT}/v1（后端启动会自动注册 ollama-chat / ollama-embedding）"
