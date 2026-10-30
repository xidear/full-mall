SHELL := /bin/bash
COMPOSE := docker compose

.PHONY: help env up up-lite up-obs down logs ps reset db-reset fix-mount-owner build build-demo build-prod test backend-test frontend-test contract-check ui-audit e2e fmt init setup start stop ops-up ops-down db-ha-up db-ha-down

help:
	@echo "Full Mall commands:"
	@echo "  make env          Copy .env.example to .env if missing"
	@echo "  make up           Start core + app + tools"
	@echo "  make up-lite      Start low-memory core services"
	@echo "  make up-obs       Start plus observability"
	@echo "  make down         Stop all"
	@echo "  make reset        Stop and remove volumes (destructive)"
	@echo "  make logs         Follow logs"
	@echo "  make build        Build backend and web"
	@echo "  make build-demo   Build demo images"
	@echo "  make build-prod   Build production images"
	@echo "  make test         Run backend + frontend tests + 接口契约对照"
	@echo "  make contract-check  前端调用 vs 后端路由（方法+路径）"
	@echo "  make e2e          Run Playwright"
	@echo "  make init         初始化宿主机 Docker/镜像/目录（需 sudo）"
	@echo "  make setup        生成配置、构建、初始化数据库并启动"
	@echo "  make start        启动（保留数据卷）"
	@echo "  make stop         停止（保留数据卷）"
	@echo "  make ops-up       启动运维/SSL 辅助容器与边缘代理"
	@echo "  make ops-down     停止运维/SSL 容器"

env:
	@test -f .env || cp .env.example .env

up: env
	$(COMPOSE) --profile core --profile app --profile tools up -d --build


# lite 是"没有 Docker 时的权宜方案"，与 core 不并行：启动前检查另一套是否在跑
check-lite-exclusive:
	@# 只检查 core 栈是否占用默认端口；tools/ai（wiremock、ollama）不算冲突
	@if docker compose ps --format '{{.Ports}}' 2>/dev/null | grep -qE ':(5432|6379|5672|7700)->'; then \
		echo "错误：core 栈正占用 5432/6379/5672/7700，请先 make down 再起 lite。"; exit 1; \
	fi

fix-mount-owner:
	@# 让 bind mount 目录属于 APP_UID/APP_GID，避免容器内 root 写宿主目录后宿主用户删不掉
	@docker run --rm -v "$$PWD/deploy:/w" alpine:3 sh -lc 'chown -R $${APP_UID:-1000}:$${APP_GID:-1000} /w'

up-lite: env check-lite-exclusive
	COMPOSE_PROJECT_NAME=fullmall-lite $(COMPOSE) -f compose.lite.yaml up -d

up-obs: env
	$(COMPOSE) --profile core --profile app --profile tools --profile obs up -d --build

down-lite:
	COMPOSE_PROJECT_NAME=fullmall-lite $(COMPOSE) -f compose.lite.yaml down

down:
	$(COMPOSE) --profile core --profile app --profile tools --profile obs --profile ai down

logs:
	$(COMPOSE) --profile core --profile app --profile tools logs -f --tail=200

ps:
	$(COMPOSE) --profile core --profile app --profile tools ps

reset:
	@echo "This removes all local volumes. Press Ctrl+C to cancel; continuing in 5s."
	@sleep 5
	$(COMPOSE) --profile core --profile app --profile tools --profile obs --profile ai down -v

db-reset:
	@echo "Dropping and recreating local database via Flyway clean is disabled. Use make reset for local Docker volumes."

build:
	cd server && mvn -q -DskipTests package
	cd web && npm run build

build-demo:
	cd server && mvn -q -DskipTests -Pdemo package

build-prod:
	cd server && mvn -q -DskipTests -Pprod package

test: backend-test frontend-test contract-check

backend-test:
	cd server && mvn -q test

frontend-test:
	cd web && npm run test

# 前端调用 vs 后端路由（方法 + 路径）：拦住"前端调了不存在的接口"这类只能在运行时暴露的问题
contract-check:
	python3 scripts/check-api-alignment.py
	node scripts/generate-permission-help.mjs --check
	# 静默兜底检查（AGENTS §1）：缺配置被默认值掩盖的地方必须登记理由
	python3 scripts/check-fallbacks.py --check

# 页面呈现审计（对比度 / 裂图 / 控制台报错 / 横向溢出）。需要 API(41280) 与 admin(41281) 已启动。
ui-audit:
	node scripts/audit-ui.mjs

# E2E 需要 API(41280)/admin(41281)/storefront(41285) 都在跑；dev-stack 用固定端口把它们起起来
e2e: dev-stack
	cd web && npm run e2e

acceptance:
	node scripts/acceptance-check.mjs

dev-stack:
	@bash scripts/dev-stack.sh start

dev-stack-stop:
	@bash scripts/dev-stack.sh stop

fmt:
	cd server && mvn -q spotless:apply
	cd web && npm run format


verify: ## 后端测试 + 打包 + 前端构建 + 接口对齐 + E2E
	scripts/verify.sh

verify-fast: ## 同上但跳过 E2E
	scripts/verify.sh --no-e2e

api-restart:
	@bash scripts/restart-api.sh

demo-ensure: ## 确保演示数据存在（幂等，不删除已有数据；恢复出厂用 make db-reset）
	@bash scripts/reset-demo-data.sh

init:
	@bash init.sh

setup:
	@bash setup.sh

start:
	@bash start.sh

stop:
	@bash stop.sh

ops-up:
	@bash start.sh --no-edge

ops-down:
	@docker compose --env-file .env.ops -f compose.ops.yaml --profile edge down

db-ha-up:
	@docker compose --env-file .env -f compose.db-ha.yaml up -d

db-ha-down:
	@docker compose --env-file .env -f compose.db-ha.yaml down
