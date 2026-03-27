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


Dashboard:
https://grafana.com/grafana/dashboards/22913-springboot-apm/
https://grafana.com/grafana/dashboards/12900-springboot-apm-dashboard/
https://grafana.com/grafana/dashboards/24605-kubernetes-deployment-pods-springboot/
https://grafana.com/grafana/dashboards