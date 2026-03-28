package com.iot.ingestor.controller;

import com.iot.commons.dto.SensorReading;
import com.iot.ingestor.service.IngestorMetrics;
import com.iot.ingestor.service.ProcessorClient;
import com.iot.ingestor.config.IngestorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Single entry point for all sensor data from device-simulator.
 * <p>
 * Responsibilities:
 * 1. Accept batches of SensorReadings via POST /api/ingest
 * 2. Validate each reading
 * 3. Forward valid readings to processor-service one by one
 * 4. Return summary of what was accepted / rejected / failed
 * <p>
 * --- Thesis relevance (HPA) ---
 * This controller is the first scaling bottleneck in the pipeline.
 * <p>
 * Under GRADUAL_RAMP:
 * t=0   → ~120 readings/batch  → thread pool comfortable → CPU ~20%
 * t=5m  → ~280 readings/batch  → thread pool filling    → CPU ~55%
 * t=8m  → ~360 readings/batch  → CPU > 70%              → HPA fires
 * t=9m  → new pod ready        → CPU drops              → HPA stabilizes
 * <p>
 * This is the core HPA lag chart in Chapter 3 of the thesis.
 */
@RestController
@RequestMapping("/api")
public class IngestorController {

    private static final Logger log = LoggerFactory.getLogger(IngestorController.class);

    private final ProcessorClient processorClient;
    private final IngestorConfig config;
    private final IngestorMetrics metrics;

    public IngestorController(ProcessorClient processorClient,
                              IngestorConfig config,
                              IngestorMetrics metrics) {
        this.processorClient = processorClient;
        this.config = config;
        this.metrics = metrics;
    }

    // ── POST /api/ingest ─────────────────────────────────────────────────

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody List<SensorReading> readings) {

        // Reject oversized batches immediately
        if (readings == null || readings.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Batch must not be empty"));
        }
        if (readings.size() > config.getMaxBatchSize()) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error", "Batch size " + readings.size() +
                                    " exceeds max " + config.getMaxBatchSize()
                    ));
        }

        metrics.receivedCounter.increment(readings.size());

        // Process the batch — track outcomes
        AtomicInteger valid = new AtomicInteger(0);
        AtomicInteger invalid = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        metrics.batchProcessTimer.record(() -> {
            for (SensorReading reading : readings) {

                // Step 1 — validate
                if (!isValid(reading)) {
                    invalid.incrementAndGet();
                    metrics.invalidCounter.increment();
                    continue;
                }

                // Step 2 — forward to processor
                boolean success = processorClient.forward(reading);
                if (success) {
                    valid.incrementAndGet();
                } else {
                    failed.incrementAndGet();
                }
            }
        });

        log.debug("Batch processed: total={} valid={} invalid={} failed={}",
                readings.size(), valid.get(), invalid.get(), failed.get());

        // Always return 202 Accepted — ingestor is non-blocking by design
        // Failures are logged and counted in Prometheus, not surfaced to simulator
        return ResponseEntity.accepted().body(Map.of(
                "received", readings.size(),
                "forwarded", valid.get(),
                "invalid", invalid.get(),
                "failed", failed.get()
        ));
    }

    // ── GET /api/health ──────────────────────────────────────────────────

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ingestor-service",
                "processorUrl", config.getProcessorUrl()
        ));
    }

    // ── GET /api/stats ───────────────────────────────────────────────────

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "maxBatchSize", config.getMaxBatchSize(),
                "processorUrl", config.getProcessorUrl(),
                "status", "running"
        ));
    }

    // ── Validation ───────────────────────────────────────────────────────

    /**
     * A reading is valid if it has:
     * - a non-null deviceId
     * - a non-null networkType
     * - a non-null timestamp
     * - a finite value (not NaN or Infinite)
     */
    private boolean isValid(SensorReading reading) {
        if (reading == null) return false;
        if (reading.getDeviceId() == null || reading.getDeviceId().isBlank()) return false;
        if (reading.getNetworkType() == null) return false;
        if (reading.getTimestamp() == 0) return false;
        return Double.isFinite(reading.getValue());
    }
}