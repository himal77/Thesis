#!/bin/bash
set -e

echo "==> Creating namespace"
kubectl apply -f namespace.yaml

echo "==> Creating secrets"
kubectl apply -f secrets.yaml

echo "==> Deploying postgres"
kubectl apply -f postgres/
kubectl rollout status statefulset/postgres -n iot

echo "==> Deploying services"
kubectl apply -f services/
kubectl rollout status deployment/ingestor-service -n iot
kubectl rollout status deployment/processor-service -n iot
kubectl rollout status deployment/alert-service -n iot
kubectl rollout status deployment/query-service -n iot
kubectl rollout status deployment/stimulator-service -n iot

echo "==> Deploying observability"
kubectl apply -f observability/

echo "==> Applying HPA and VPA"
kubectl apply -f processor-hpa.yaml
kubectl apply -f alert-vpa.yaml

echo "==> Deploying load test"
kubectl apply -f ../../load-test/k8s/

echo ""
echo "Done. Access:"
echo "  Grafana:    http://localhost:30030"
echo "  Prometheus: http://localhost:30090"
echo "  Locust UI:  http://localhost:30089"
