ENV_DEV  = --env-file infra/env/.env.dev
ENV_TEST = --env-file infra/env/.env.test

INFRA    = -f infra/docker/docker-compose-infra.yaml
SERVICES = -f infra/docker/docker-compose-services.yaml

KUBECTL  = kubectl
K8S_NS   = iot
K8S_DIR  = infra/k8s

.PHONY: dev test down-dev down-test reload logs \
        k8s-deploy k8s-infra k8s-services k8s-scale \
        k8s-down k8s-status k8s-logs k8s-hpa k8s-vpa-check help

# ─────────────────────────────────────────────
# Local Dev / Docker Compose
# ─────────────────────────────────────────────

## dev        — Infra only in Docker, run services in IntelliJ
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

# ─────────────────────────────────────────────
# Kubernetes
# ─────────────────────────────────────────────

## k8s-deploy  — Full deploy: namespace → infra → services → HPA → VPA
k8s-deploy:
	$(KUBECTL) apply -f $(K8S_DIR)/00-namespace.yaml
	$(KUBECTL) apply -f $(K8S_DIR)/configmap/
	$(KUBECTL) apply -f $(K8S_DIR)/infra/
	@echo "⏳ Waiting for config-server to be ready..."
	$(KUBECTL) rollout status deployment/config-server -n $(K8S_NS) --timeout=120s
	$(KUBECTL) apply -f $(K8S_DIR)/services/
	$(KUBECTL) apply -f $(K8S_DIR)/hpa/
	$(KUBECTL) apply -f $(K8S_DIR)/vpa/
	@echo "✅ Full stack deployed to Kubernetes"

## k8s-infra   — Deploy infra only (postgres, prometheus, grafana, config-server)
k8s-infra:
	$(KUBECTL) apply -f $(K8S_DIR)/00-namespace.yaml
	$(KUBECTL) apply -f $(K8S_DIR)/configmap/
	$(KUBECTL) apply -f $(K8S_DIR)/infra/

## k8s-services — Deploy application services only
k8s-services:
	$(KUBECTL) apply -f $(K8S_DIR)/services/
	$(KUBECTL) apply -f $(K8S_DIR)/hpa/
	$(KUBECTL) apply -f $(K8S_DIR)/vpa/

## k8s-down    — Remove all K8s resources (keeps namespace)
k8s-down:
	$(KUBECTL) delete -f $(K8S_DIR)/vpa/    2>/dev/null || true
	$(KUBECTL) delete -f $(K8S_DIR)/hpa/    2>/dev/null || true
	$(KUBECTL) delete -f $(K8S_DIR)/services/ 2>/dev/null || true
	$(KUBECTL) delete -f $(K8S_DIR)/infra/  2>/dev/null || true

## k8s-status  — Show pods, HPA, and VPA status
k8s-status:
	@echo "\n── Pods ──────────────────────────────────"
	$(KUBECTL) get pods -n $(K8S_NS) -o wide
	@echo "\n── HPA ───────────────────────────────────"
	$(KUBECTL) get hpa -n $(K8S_NS)
	@echo "\n── VPA ───────────────────────────────────"
	$(KUBECTL) get vpa -n $(K8S_NS) 2>/dev/null || echo "VPA not installed"

## k8s-hpa     — Watch HPA scaling in real time
k8s-hpa:
	$(KUBECTL) get hpa -n $(K8S_NS) -w

## k8s-vpa-check — Show VPA memory recommendations for alert-engine
k8s-vpa-check:
	$(KUBECTL) describe vpa alert-engine-vpa -n $(K8S_NS)

## k8s-logs    — Tail logs of a service  (usage: make k8s-logs SVC=ingestor-service)
k8s-logs:
	$(KUBECTL) logs -f deployment/$(SVC) -n $(K8S_NS)

## k8s-scale   — Manually scale a deployment (usage: make k8s-scale SVC=ingestor-service N=4)
k8s-scale:
	$(KUBECTL) scale deployment/$(SVC) --replicas=$(N) -n $(K8S_NS)

## k8s-load    — Run a quick load test against ingestor-service to trigger HPA
k8s-load:
	$(KUBECTL) run load-test --image=busybox --rm -it -n $(K8S_NS) -- \
		/bin/sh -c "while true; do wget -q -O- http://ingestor-service:8080/actuator/health; done"

## help        — Show all commands
help:
	@grep -E '^##' Makefile | sed 's/## /  make /'
