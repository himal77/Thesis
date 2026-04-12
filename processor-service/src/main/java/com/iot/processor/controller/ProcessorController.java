package com.iot.processor.controller;


import com.iot.commons.dto.ProcessedReading;
import com.iot.processor.config.ProcessorConfig;
import com.iot.processor.entity.ReadingEntity;
import com.iot.processor.repository.ReadingRepository;
import com.iot.processor.service.AlertEngineClient;
import com.iot.processor.service.AnomalyDetector;
import com.iot.processor.service.ReadingClassifier;
import com.iot.commons.dto.SensorReading;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

/**
 * Main entry point for the processor-service.
 *
 * Every POST /api/process call:
 *   1. computeZScore()  → O(windowSize) CPU work
 *   2. normalize()      → trivial
 *   3. classify()       → trivial
 *   4. save to Postgres → I/O
 *   5. forward to alert-engine → I/O
 *
 * Step 1 is what saturates CPU under burst load and triggers HPA.
 * The timer metric "processor.processing.duration" captures exactly
 * how long each reading takes — visible in Grafana during experiments.
 */
@RestController
@RequestMapping("/api")
public class ProcessorController {

    private final AnomalyDetector    anomalyDetector;
    private final ReadingClassifier  classifier;
    private final ReadingRepository  repository;
    private final AlertEngineClient  alertClient;
    private final ProcessorConfig    processorConfig;
    private final Counter            processedCounter;
    private final Counter            anomalyCounter;
    private final Timer              processingTimer;

    public ProcessorController(AnomalyDetector anomalyDetector,
                               ReadingClassifier classifier,
                               ReadingRepository repository,
                               AlertEngineClient alertClient,
                               ProcessorConfig processorConfig,
                               MeterRegistry registry) {
        this.anomalyDetector  = anomalyDetector;
        this.classifier       = classifier;
        this.repository       = repository;
        this.alertClient      = alertClient;
        this.processorConfig  = processorConfig;
        this.processedCounter = registry.counter("processor.readings.processed");
        this.anomalyCounter   = registry.counter("processor.readings.anomalies");
        this.processingTimer  = registry.timer("processor.processing.duration");
    }

    @PostMapping("/process")
    public ResponseEntity<?> process(@RequestBody SensorReading reading) {
        ProcessedReading result = processingTimer.record(() -> doProcess(reading));
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "trackedDevices", anomalyDetector.getTrackedDeviceCount(),
                "status",         "running"
        ));
    }

    private ProcessedReading doProcess(SensorReading reading) {
        // CPU heavy: sliding window Z-score
        double zScore     = anomalyDetector.computeZScore(reading);

        // classify severity
        String category   = classifier.classify(zScore);

        // build result
        boolean isAnomaly = Math.abs(zScore) > processorConfig.getAnomalyThreshold();
        ProcessedReading result = new ProcessedReading(
                reading, zScore, category, isAnomaly, Instant.now());

        // persist to PostgreSQL
        repository.save(toEntity(result));

        // forward anomalies to alert-engine (fire and forget)
        if (isAnomaly) {
            anomalyCounter.increment();
            alertClient.forward(result);
        }

        processedCounter.increment();
        return result;
    }

    private ReadingEntity toEntity(ProcessedReading r) {
        ReadingEntity e = new ReadingEntity();
        e.setDeviceId(r.getSensorReading().getDeviceId());
        e.setNetworkType(r.getSensorReading().getNetworkType());
        e.setValue(r.getSensorReading().getValue());
        e.setZScore(r.getZScore());
        e.setCategory(r.getCategory());
        e.setAnomaly(r.isAnomaly());
        e.setTimestamp(Instant.ofEpochSecond(r.getSensorReading().getTimestamp()));
        e.setProcessedAt(r.getProcessedAt());
        return e;
    }
}
