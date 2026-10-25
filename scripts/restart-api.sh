#!/usr/bin/env bash
# 重启后端：**先确保 jar 比源码新**，否则自动重新打包。
# 用途：任何"改了代码要验证"的场景都走这个脚本，避免再出现"跑的是旧 jar"。
set -euo pipefail

# Maven/JDK 来自 ~/.dsh-java-env；脚本必须自己加载，不能假设调用方已 source
if [[ -f "$HOME/.dsh-java-env" ]]; then
  # shellcheck disable=SC1091
  source "$HOME/.dsh-java-env"
fi

root="$(cd "$(dirname "$0")/.." && pwd)"
cd "$root/server"

jar="target/fullmall-server-0.1.0-SNAPSHOT.jar"

needs_build=0
if [[ ! -f "$jar" ]]; then
  echo "[restart-api] jar 不存在，需要打包"
  needs_build=1
else
  newer="$(find src pom.xml -newer "$jar" -print -quit || true)"
  if [[ -n "$newer" ]]; then
    echo "[restart-api] 源码比 jar 新（$newer），需要重新打包"
    needs_build=1
  fi
fi

if [[ "$needs_build" == "1" ]]; then
  echo "[restart-api] mvn package ..."
  mvn -B -DskipTests package -q
else
  echo "[restart-api] jar 已是最新"
fi

# 停旧进程（只杀本项目的 jar，不动别的 java）
pids="$(pgrep -f "java -jar .*fullmall-server-0\.1\.0-SNAPSHOT\.jar" || true)"
if [[ -n "$pids" ]]; then
  echo "[restart-api] 停止旧进程: $pids"
  kill $pids 2>/dev/null || true
  for _ in $(seq 1 20); do
    pgrep -f "java -jar .*fullmall-server-0\.1\.0-SNAPSHOT\.jar" >/dev/null || break
    sleep 0.5
  done
  # 进程可能已在此期间退出：kill 失败不算错误，否则 set -e 会中断后续启动
  pgrep -f "java -jar .*fullmall-server-0\.1\.0-SNAPSHOT\.jar" | xargs -r kill -9 || true
fi

: "${APP_SECRET_KEY:=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=}"
export APP_SECRET_KEY
export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-dev}"

echo "[restart-api] 启动新 jar ..."
PORT="${APP_PORT:-41280}"
nohup java -jar "$jar" --server.port="$PORT" > /tmp/fullmall-api.log 2>&1 &

api_ready() {
  python3 -c "import urllib.request,os; urllib.request.urlopen('http://localhost:'+os.environ.get('APP_PORT','41280')+'/api/v1/products?size=1', timeout=3).read()" >/dev/null 2>&1
}

for _ in $(seq 1 90); do
  if api_ready; then break; fi
  sleep 1
done

if ! api_ready; then
  echo "[restart-api] 90s 内未就绪，最后的日志："
  tail -40 /tmp/fullmall-api.log
  exit 1
fi

echo "[restart-api] 就绪：http://localhost:${PORT}"
