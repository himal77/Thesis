package com.iot.ingestor.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * All Prometheus metrics for ingestor-service in one place.
 *
 * --- Thesis relevance (HPA) ---
 * These metrics are what Grafana plots during Experiment 1.
 *
 * Key metrics to watch:
 *   ingestor.readings.received  → total throughput from simulator
 *   ingestor.readings.forwarded → successfully sent to processor
 *   ingestor.readings.invalid   → dropped due to validation failure
 *   ingestor.readings.failed    → processor unreachable
 *   ingestor.forward.duration   → how long each processor call takes
 *                                  rises under load → CPU climbs → HPA fires
 */
@Component
public class IngestorMetrics {

    public final Counter receivedCounter;
    public final Counter forwardedCounter;
    public final Counter invalidCounter;
    public final Counter failedCounter;
    public final Timer   forwardTimer;
    public final Timer   batchProcessTimer;

    public IngestorMetrics(MeterRegistry registry) {
        this.receivedCounter   = registry.counter("ingestor.readings.received");
        this.forwardedCounter  = registry.counter("ingestor.readings.forwarded");
        this.invalidCounter    = registry.counter("ingestor.readings.invalid");
        this.failedCounter     = registry.counter("ingestor.readings.failed");
        this.forwardTimer      = registry.timer("ingestor.forward.duration");
        this.batchProcessTimer = registry.timer("ingestor.batch.duration");
    }
}
