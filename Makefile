ENV_DEV  = --env-file infra/env/.env.dev
ENV_TEST = --env-file infra/env/.env.test

INFRA    = -f infra/docker/docker-compose-infra.yaml
SERVICES = -f infra/docker/docker-compose-services.yaml

.PHONY: dev test down-dev down-test reload logs help

dev:
	docker compose $(ENV_DEV) $(INFRA) up -d
	@echo "✅ Prometheus : http://localhost:9090"
	@echo "✅ Grafana    : http://localhost:3000"
	@echo "✅ Postgres   : localhost:5432"
	@echo "▶  Start your services in IntelliJ"

## test       — Full stack in Docker
test:
	docker compose $(ENV_TEST) $(INFRA) up -d
	docker compose $(ENV_TEST) $(SERVICES) up -d
	@echo "✅ All services running in Docker"
	@echo "✅ Prometheus : http://localhost:9090"
	@echo "✅ Grafana    : http://localhost:3000"

## down-dev   — Stop dev stack
down-dev:
	docker compose $(ENV_DEV) $(INFRA) down

## down-test  — Stop test stack
down-test:
	docker compose $(ENV_TEST) $(SERVICES) down 2>/dev/null || true
	docker compose $(ENV_TEST) $(INFRA) down

## reload     — Reload Prometheus config without restarting
reload:
	curl -s -X POST http://localhost:9090/-/reload && echo "✅ Prometheus reloaded"

## logs       — Tail Prometheus + Grafana logs
logs:
	docker compose $(ENV_DEV) $(INFRA) logs -f prometheus grafana

## help       — Show all commands
help:
	@grep -E '^##' Makefile | sed 's/## /  make /'