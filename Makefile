SHELL := /bin/bash
COMPOSE := docker compose

.PHONY: help env up up-lite up-obs down logs ps reset db-reset fix-mount-owner build build-demo build-prod test backend-test frontend-test e2e fmt

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
	@echo "  make test         Run backend + frontend tests"
	@echo "  make e2e          Run Playwright"

env:
	@test -f .env || cp .env.example .env

up: env
	$(COMPOSE) --profile core --profile app --profile tools up -d --build


# lite 是"没有 Docker 时的权宜方案"，与 core 不并行：启动前检查另一套是否在跑
check-lite-exclusive:
	@if [ -n "$$(docker compose ps -q 2>/dev/null)" ]; then \
		echo "错误：core 正在运行（fullmall-*）。lite 只用于无 Docker 环境，请先 make down。"; exit 1; \
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

test: backend-test frontend-test

backend-test:
	cd server && mvn -q test

frontend-test:
	cd web && npm run test

e2e:
	cd web && npm run e2e

fmt:
	cd server && mvn -q spotless:apply
	cd web && npm run format


verify: ## 后端测试 + 打包 + 前端构建 + 接口对齐 + E2E
	scripts/verify.sh

verify-fast: ## 同上但跳过 E2E
	scripts/verify.sh --no-e2e

api-restart:
	@bash scripts/restart-api.sh
