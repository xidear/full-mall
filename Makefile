SHELL := /bin/bash
COMPOSE := docker compose

.PHONY: help env up up-lite up-obs down logs ps reset db-reset build build-demo build-prod test backend-test frontend-test e2e fmt

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

up-lite: env
	$(COMPOSE) -f compose.lite.yaml up -d

up-obs: env
	$(COMPOSE) --profile core --profile app --profile tools --profile obs up -d --build

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