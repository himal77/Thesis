┌─────────────────────────────────────────────────────────────┐
│                        Kind Cluster                         │
│                                                             │
│  ┌──────────────┐   HTTP/REST    ┌─────────────────────┐    │
│  │    Device    │ ─────────────▶ │   Ingestor          │    │
│  │  Simulator   │                │   Service           │    │
│  │  (Java)      │                │   (I/O bound)       │    │
│  └──────────────┘                │   HPA → CPU         │    │ 
│                                  └────────┬────────────┘    │
│                                           │ internal HTTP   │
│                                           ▼                 │
│                                  ┌─────────────────────┐    │
│                                  │   Processor         │    │
│                                  │   Service           │    │
│                                  │   (CPU bound)       │    │
│                                  │   HPA → CPU         │    │
│                                  └────────┬────────────┘    │
│                                           │                 │
│                          ┌────────────────┼──────────┐      │
│                          ▼                ▼          ▼      │ 
│                   ┌────────────┐  ┌────────────┐  ┌─────┐   │
│                   │  Alert     │  │  Query     │  │Redis│   │
│                   │  Engine    │  │  API       │  │     │   │
│                   │ (Mem bound)│  │ (mixed)    │  └─────┘   │
│                   │ VPA        │  │ HPA → RPS  │            │
│                   └────────────┘  └────────────┘            │
│                          │                │                 │
│                          └───────┬────────┘                 │
│                                  ▼                          │
│                            PostgreSQL                       │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │           Prometheus + Grafana + metrics-server      │   │
│  └──────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘

# EdgeStorm — IoT Kubernetes Scaling Benchmark

> **Thesis:** "Beyond CPU Metrics: Evaluating Kubernetes Autoscaling Strategies for Bursty IoT Telemetry Pipelines"

---

## Thesis Structure

| Chapter | Topic |
|---|---|
| 1 | Introduction, problem statement, research questions |
| 2 | Background — Kubernetes architecture, HPA/VPA/CA theory |
| 3 | Horizontal Pod Autoscaler (HPA) deep dive |
| 4 | Vertical Pod Autoscaler (VPA) deep dive |
| 5 | Cluster Autoscaler (CA) deep dive |
| 6 | Comparative analysis & interaction effects |
| 7 | Experimental design & benchmark (EdgeStorm) |
| 8 | Results & discussion |
| 9 | Proposed adaptive scaling model |
| 10 | Conclusions |

---

## Project Architecture

```
device-simulator
      │
      │ HTTP batch (POST /api/ingest)
      ▼
ingestor-service  ──── HTTP/reading ────▶  processor-service  ──── HTTP/processed ────▶  alert-engine
HPA · I/O bound                            HPA · CPU bound                               VPA · memory bound
                                                  │                                             │
                                               write                                        write alerts
                                                  └─────────────────┬───────────────────────────┘
                                                                    ▼
                                                              PostgreSQL
                                                                    │
                                                                  read
                                                                    ▼
                                                              query-api  ◀── Grafana
                                                              HPA · mixed
```

### Service Ports

| Service | Port | Scaler | Resource profile |
|---|---|---|---|
| device-simulator | 8080 | none (load gen) | — |
| ingestor-service | 8081 | HPA on CPU | I/O bound |
| processor-service | 8082 | HPA on CPU | CPU bound |
| alert-engine | 8083 | VPA on memory | memory bound |
| query-api | 8084 | HPA on CPU | mixed |
| PostgreSQL | 5432 | StatefulSet | not scaled |

---

## Common Module

```
common/src/main/java/com/edgestorm/common/
├── model/
│   ├── NetworkType.java        TRAFFIC · ENVIRONMENT · ENERGY · INDUSTRIAL
│   └── TrafficProfile.java     BASELINE · GRADUAL_RAMP · SAWTOOTH · FLEET_GROWTH
│                               SUDDEN_SPIKE · SUSTAINED_MAX · CASCADE
└── dto/
    ├── SensorReading.java      raw reading from simulator
    ├── ProcessedReading.java   enriched by processor (zScore, category, anomaly)
    └── AlertDto.java           fired by alert-engine
```

**Rule:** only DTOs passed between services belong in common. JPA entities stay in their owning service.

---

## Service 1 — device-simulator

**Role:** generates synthetic IoT traffic, sends batches to ingestor.

### Key classes

```
DeviceSimulator.java          @Scheduled every 500ms, calls buildBatch()
SimulatorController.java      POST /api/profile to switch traffic profile at runtime
SimulatorConfig.java          reads env vars (ingestor.url, devices-per-network)
RampTraffic.java              time-based multiplier 1→10 over N minutes
SawtoothTraffic.java          alternates spike/idle on a cycle
FleetGrowth.java              grows device count over time (100→1000)
```

### buildBatch logic

```java
// for each NetworkType: count = max(1, (devicesPerNetwork / 10) * multiplier)
// 10% of fleet reports per tick — realistic IoT behaviour
// multiplier compresses the interval during burst scenarios
```

### Batch sizes at devicesPerNetwork=100

| Profile | Multiplier | Readings/tick | Readings/sec |
|---|---|---|---|
| BASELINE | 3 | 120 | 240 |
| GRADUAL_RAMP | 1→10 | 40→400 | 80→800 |
| SAWTOOTH | 1 or 10 | 40 or 400 | 80 or 800 |
| FLEET_GROWTH | 1, devices grow | 40→400 | 80→800 |
| SUDDEN_SPIKE / CASCADE / SUSTAINED_MAX | 20 | 800 | 1600 |

### Traffic profile switch

```bash
curl -X POST http://localhost:8080/api/profile \
     -H "Content-Type: application/json" \
     -d '{"profile":"GRADUAL_RAMP"}'
```

Time-based profiles (GRADUAL_RAMP, SAWTOOTH, FLEET_GROWTH) call `.start()` to record start time.
Stateless profiles (BASELINE, SUDDEN_SPIKE, CASCADE, SUSTAINED_MAX) need no initialisation.
**Always reset all time-based profiles before switching** to prevent stale state leaking between experiments.

### SensorReading.random() — fixed device pool (important bug fix)

```java
// WRONG — generates new random deviceId every call → windows never fill → zScore always 0
String deviceId = type.name().toLowerCase() + "-" + rnd.nextInt(1000);

// CORRECT — fixed pool of 1000 IDs per network type, reused across calls
private static final Map<NetworkType, List<String>> DEVICE_POOL = ...
String deviceId = pool.get(rnd.nextInt(pool.size()));
```

---

## Service 2 — ingestor-service

**Role:** validates batches from simulator, forwards each reading to processor-service one by one.

**Why I/O bound:** receives 1 batch → makes N individual HTTP calls → thread pool fills → CPU rises → HPA fires.

### Key classes

```
IngestorController.java   POST /api/ingest — validates + fans out to processor
ProcessorClient.java      HTTP client, one call per reading (intentional fan-out)
IngestorMetrics.java      Prometheus counters: received, forwarded, invalid, failed
IngestorConfig.java       processor.url, max-batch-size
```

### Validation rules

- `deviceId` not null or blank
- `networkType` not null
- `timestamp` not null
- `value` must be finite (not NaN or Infinite)

### HPA behaviour during experiments

```
GRADUAL_RAMP:
  t=0   → ~120 readings/batch → CPU ~20% → no scaling
  t=5m  → ~280 readings/batch → CPU ~55% → approaching threshold
  t=8m  → ~360 readings/batch → CPU >70% → HPA fires
  t=9m  → new pod ready       → CPU drops → stabilises

SUDDEN_SPIKE:
  t=0   → 800 readings/batch → CPU spikes instantly
  t=45s → HPA fires (metric collection lag)
  t=90s → new pod ready (scheduling + JVM warmup ~15s)
  = 90s of degraded latency with no scaling response
```

---

## Service 3 — processor-service

**Role:** CPU-heavy enrichment — Z-score anomaly detection, normalisation, classification. Writes to PostgreSQL, forwards anomalies to alert-engine.

### Key classes

```
ProcessorController.java    POST /api/process — orchestrates pipeline
AnomalyDetector.java        sliding window Z-score per device (CPU heavy)
ReadingNormalizer.java      scales value to 0.0–1.0 by network type
ReadingClassifier.java      NORMAL / LOW / MEDIUM / HIGH / CRITICAL
AlertEngineClient.java      fire-and-forget HTTP forward to alert-engine
entity/ReadingEntity.java   JPA entity (owns the schema, ddl-auto=update)
repository/ReadingRepository.java
```

### Z-score explained

```
formula:  z = (value − mean) / stdDev

|z| < 1.5   → NORMAL
|z| 1.5–2.5 → LOW
|z| 2.5–3.0 → MEDIUM  ← anomaly threshold
|z| 3.0–4.0 → HIGH
|z| ≥ 4.0   → CRITICAL
```

Z-score is always computed against that device's own history window (100 readings). A reading of 145 km/h on a slow street is anomalous; the same value on a motorway sensor is normal.

### AnomalyDetector — two critical bugs fixed

**Bug 1 — random deviceId prevents window accumulation:**
`SensorReading.random()` must use a fixed device pool, not `rnd.nextInt()` each call.

**Bug 2 — ArrayDeque is not thread-safe:**
```java
synchronized (window) {
    // compute zScore
    // evict oldest if full
    // add new value
}
// synchronize per device — traffic-42 and traffic-99 don't block each other
```

**Bug 3 — minimum window before computing:**
```java
private static final int MIN_WINDOW_FOR_ZSCORE = 10;
// reduces warmup from ~100k readings to ~10k readings system-wide
```

### Processing pipeline per reading

```
1. computeZScore()   → O(windowSize) CPU work  ← HPA trigger
2. normalize()       → scales value 0.0–1.0
3. classify()        → maps |zScore| to severity string
4. save to Postgres  → I/O
5. if anomaly: forward to alert-engine  → I/O (fire and forget)
```

---

## Service 4 — alert-engine

**Role:** stateful anomaly evaluation per device. Memory grows with fleet size — not with RPS. VPA right-sizes memory over time.

### Key classes

```
AlertEngineController.java   POST /api/evaluate — entry point
AlertRuleEvaluator.java      3-rule gate before firing an alert
DeviceStateManager.java      ConcurrentHashMap<deviceId, DeviceState>
DeviceState.java             per-device history + consecutive counter + cooldown
entity/AlertEntity.java      JPA entity (owns alerts table)
repository/AlertRepository.java
```

### Alert rules (all 3 must pass)

1. Reading must be flagged as anomaly (`|zScore| > 2.5`)
2. Device must have N consecutive anomalies (default 3) — prevents noise spikes
3. Device must not be in cooldown (default 60s) — prevents alert flooding

### Why VPA and not HPA

```
Memory usage = f(fleet size), NOT f(RPS)
  100 devices  → ~160KB  state
  500 devices  → ~800KB  state
  1000 devices → ~1.6MB  state

CPU usage = near-constant regardless of fleet size

HPA watching CPU would never trigger — even at 1000 devices CPU stays low.
VPA watches container_memory_usage_bytes over time and adjusts requests.memory.
```

### VPA progression during E2 (fleet growth)

```
t=0   → 100 devices  → VPA recommends 256Mi
t=10m → 300 devices  → VPA recommends 320Mi → pod restart
t=20m → 500 devices  → VPA recommends 420Mi → pod restart
t=60m → 1000 devices → VPA stabilises at ~512Mi
```

Restart events during cascade (E4) = the most dangerous interaction in the thesis.

---

## Service 5 — query-api

**Role:** read-only REST API over PostgreSQL. Consumed by Grafana dashboards.

### Key classes

```
ReadingQueryController.java   GET /api/readings/recent, /device/{id}, /stats
AlertQueryController.java     GET /api/alerts/recent, /device/{id}, /stats
StatsController.java          GET /api/stats/overview — 4 DB queries per call
entity/ReadingEntity.java     read-only view (no setters, ddl-auto=validate)
entity/AlertEntity.java       read-only view
```

**ddl-auto=validate** — query-api never modifies the schema. Processor and alert-engine own their tables.

### Why mixed HPA

Each Grafana dashboard poll triggers multiple aggregation queries. Under concurrent polling during experiments: CPU climbs (query planning) + I/O climbs (DB reads) → both signals visible → HPA scales on CPU.

---

## Experiments

### E1 — HPA reactive lag

3 traffic shapes applied to ingestor + processor:

| Run | Profile | Duration | What it shows |
|---|---|---|---|
| E1-A | GRADUAL_RAMP | 20 min | HPA tracking smooth load increase |
| E1-B | SUDDEN_SPIKE | 20 min | 45–90s lag window clearly visible |
| E1-C | SAWTOOTH | 30 min | cooldown over-provisioning between spikes |

**Key metric:** time from CPU > threshold → first new pod Ready

### E2 — VPA right-sizing

Fleet grows 100 → 1000 devices via FLEET_GROWTH profile over 60 min.

**Key metrics:** `container_memory_usage_bytes`, `kube_vpa_status_recommendation`, OOMKill count

### E3 — Cluster Autoscaler node pressure

SUSTAINED_MAX held until HPA maxes pods on both nodes → Pending pods → CA adds node.

```
t=0    → burst starts
t=90s  → HPA maxed out, pods enter Pending
t=270s → CA adds node, pods scheduled
= 3 min of Pending pods = potential message loss
```

**Key finding:** node provisioning (2–4 min) is the real bottleneck, not pod scheduling (10s).

### E4 — The cascade (all three scalers)

All traffic profiles fire at t=0. Measures interaction between HPA, VPA, CA.

```
t=0    → CASCADE burst
t=45s  → ingestor HPA scales
t=60s  → processor HPA scales → node capacity exhausted
t=90s  → CA triggered
t=105s → VPA restarts alert-engine mid-cascade (worst case)
t=270s → CA adds node, system recovers
```

**Key finding:** VPA restarts are unaware of ongoing HPA/CA events — 30s alert gap during cascade.

### E5 — HPA threshold sensitivity

Same SUDDEN_SPIKE applied 3× with different `targetCPUUtilizationPercentage`:

| Config | Threshold | Scale-up lag | Peak p99 | Pod waste |
|---|---|---|---|---|
| A | 50% (aggressive) | 25s | 180ms | high |
| B | 70% (balanced) | 45s | 340ms | medium |
| C | 90% (lazy) | 90s | 1200ms | low |

**Output:** cost vs latency tradeoff curve → actionable tuning recommendation.

---

## Expected Conclusions

1. **HPA has a structural 45–90s lag** — unavoidable due to metric collection + JVM warmup. Only gradual ramps are handled gracefully.

2. **VPA is necessary but disruptive** — memory-bound stateful services cannot be managed by HPA at all. VPA restarts create reliability risk during concurrent scaling events.

3. **CA is the real bottleneck, not HPA** — pod scaling is fast (~45s), node provisioning is slow (2–4 min). Any architecture that can exhaust node capacity faces guaranteed multi-minute degradation.

4. **The three scalers interact, and not always well** — HPA + CA compose cleanly. VPA does not — it restarts pods regardless of ongoing scale events. VPA in `Off` mode (advisor only) is safer in production.

5. **Workload shape determines the optimal scaler** — CPU for compute-bound, RPS for I/O-bound, memory-over-time for stateful. Wrong signal = no scaling response regardless of load.

---

## Z-score Quick Reference

```
z = (value − mean) / stdDev

Per-device sliding window (size=100, min=10 readings to compute)
Synchronized per device — not global lock

|z| < 1.5   NORMAL
|z| 1.5–2.5 LOW
|z| 2.5–3.0 MEDIUM  ← anomaly flag (isAnomaly=true)
|z| 3.0–4.0 HIGH
|z| ≥ 4.0   CRITICAL
```

---

## Project Structure

```
edgestorm/
├── pom.xml                        parent POM, 6 modules
├── common/                        shared DTOs + enums, zero Spring deps
├── device-simulator/              port 8080
├── ingestor-service/              port 8081
├── processor-service/             port 8082
├── alert-engine/                  port 8083
├── query-api/                     port 8084
├── k8s/
│   ├── base/                      Deployments, Services, ConfigMaps
│   ├── hpa/                       HPA manifests (ingestor, processor, query-api)
│   ├── vpa/                       VPA manifest (alert-engine)
│   └── cluster-autoscaler/        CA deployment
├── monitoring/
│   ├── prometheus/
│   └── grafana/dashboards/
├── experiments/
│   ├── exp1_hpa_lag.sh
│   ├── exp2_vpa_fleet.sh
│   ├── exp3_ca_nodes.sh
│   ├── exp4_cascade.sh
│   └── exp5_thresholds.sh
└── results/
    ├── raw/
    ├── processed/
    └── notebooks/
```

---

## Build Order

```bash
# 1. build common first — all other modules depend on it
mvn install -pl common

# 2. build all modules
mvn clean package

# 3. run locally (no K8s)
docker-compose up

# 4. deploy to Kind
kubectl apply -f k8s/base/
kubectl apply -f k8s/hpa/
kubectl apply -f k8s/vpa/
```

---

## Key Prometheus Metrics

| Metric | Source | Used in |
|---|---|---|
| `ingestor.readings.received` | ingestor | E1 throughput chart |
| `processor.processing.duration` | processor | E1 latency chart |
| `processor.readings.anomalies` | processor | E2 anomaly rate |
| `container_memory_usage_bytes` | kubelet | E2 VPA tracking |
| `kube_vpa_status_recommendation` | VPA | E2 recommendation drift |
| `kube_pod_status_phase{phase="Pending"}` | kube-state-metrics | E3 CA trigger |
| `cluster_autoscaler_scaled_up_nodes_total` | CA | E3 node provisioning |
| `kube_pod_container_status_restarts_total` | kube-state-metrics | E4 VPA restarts |
| `http_server_requests_seconds` | all services | E5 p99 latency |

Dashboard:
https://grafana.com/grafana/dashboards/22913-springboot-apm/
https://grafana.com/grafana/dashboards/12900-springboot-apm-dashboard/
https://grafana.com/grafana/dashboards/24605-kubernetes-deployment-pods-springboot/
https://grafana.com/grafana/dashboards