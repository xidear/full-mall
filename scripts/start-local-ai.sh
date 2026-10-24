#!/usr/bin/env bash
# 启动本机 AI（Ollama）：0.xB 文本模型 + embedding，纳入自有管理。
# 幂等：已启动则复用；模型已存在则跳过拉取；最后做一次真实推理自检。
set -euo pipefail

TEXT_MODEL="${TEXT_MODEL:-qwen2.5:0.5b}"
EMB_MODEL="${EMB_MODEL:-nomic-embed-text}"
PORT="${OLLAMA_PORT:-11434}"

cd "$(dirname "$0")/.."

echo "[ai] 启动 ollama 容器 ..."
docker compose --profile ai up -d ollama

for i in $(seq 1 30); do
  if curl -fsS "http://localhost:${PORT}/api/tags" >/dev/null 2>&1; then break; fi
  sleep 1
done

echo "[ai] 拉取模型 ${TEXT_MODEL} / ${EMB_MODEL} ..."
docker exec fullmall-ollama-1 ollama pull "$TEXT_MODEL" >/dev/null
docker exec fullmall-ollama-1 ollama pull "$EMB_MODEL" >/dev/null

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
