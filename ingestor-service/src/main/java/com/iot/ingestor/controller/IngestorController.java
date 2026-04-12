package com.iot.ingestor.controller;

import com.iot.commons.dto.SensorReading;
import com.iot.ingestor.service.IngestorMetrics;
import com.iot.ingestor.service.ProcessorClient;
import com.iot.ingestor.config.IngestorConfig;
import io.micrometer.common.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
 *
 * This controller is the first scaling bottleneck in the pipeline.
 * <p>
 * Under GRADUAL_RAMP:
 * t=0   → ~120 readings/batch  → thread pool comfortable → CPU ~20%
 * t=5m  → ~280 readings/batch  → thread pool filling    → CPU ~55%
 * t=8m  → ~360 readings/batch  → CPU > 70%              → HPA fires
 * t=9m  → new pod ready        → CPU drops              → HPA stabilizes
 * <p>
 * This is the core HPA lag chart
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

    @PostMapping("/ingest")
    public ResponseEntity<?> ingest(@RequestBody List<SensorReading> readings) {
        if (CollectionUtils.isEmpty(readings)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Batch must not be empty"));
        }

        metrics.receivedCounter.increment(readings.size());

        // Process the batch — track outcomes
        AtomicInteger valid = new AtomicInteger(0);
        AtomicInteger invalid = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        metrics.batchProcessTimer.record(() -> {
            for (SensorReading reading : readings) {

                if (!isValid(reading)) {
                    invalid.incrementAndGet();
                    metrics.invalidCounter.increment();
                    continue;
                }

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

        return ResponseEntity.accepted().body(Map.of(
                "received", readings.size(),
                "forwarded", valid.get(),
                "invalid", invalid.get(),
                "failed", failed.get()
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "processorUrl", config.getProcessorUrl(),
                "status", "running",
                "receivedData", metrics.receivedCounter.count(),
                "forwardedData", metrics.forwardedCounter.count(),
                "invalidData", metrics.invalidCounter.count(),
                "forwardFailedData", metrics.failedCounter.count(),
                "averageForwardTime", metrics.forwardTimer.mean(TimeUnit.MICROSECONDS),
                "averageBatchProcessTime", metrics.batchProcessTimer.mean(TimeUnit.MICROSECONDS)
        ));
    }

    private boolean isValid(SensorReading reading) {
        if (ObjectUtils.isEmpty(reading)                    ||
            StringUtils.isBlank(reading.getDeviceId())      ||
            ObjectUtils.isEmpty(reading.getNetworkType())   ||
            reading.getTimestamp() == 0) {
            return false;
        }

        return Double.isFinite(reading.getValue());
    }
}