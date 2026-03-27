package com.IoT.alert.controller;

import com.IoT.alert.AlertEntity;
import com.IoT.alert.AlertRepository;
import com.IoT.alert.AlertRuleEvaluator;
import com.IoT.alert.DeviceStateManager;
import com.IoT.commons.dto.AlertDto;
import com.IoT.commons.dto.ProcessedReading;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Receives ProcessedReading from processor-service,
 * evaluates alert rules, persists fired alerts to PostgreSQL.
 *
 * --- Thesis relevance (VPA) ---
 * This pod is NOT scaled by request rate or CPU.
 * It is scaled by MEMORY — which grows with fleet size via
 * DeviceStateManager. VPA watches container memory usage over
 * time and adjusts requests.memory upward as the fleet grows.
 *
 * Key Grafana metrics to watch during E2 (fleet growth):
 *   container_memory_usage_bytes{pod=~"alert-engine.*"}
 *   kube_vpa_status_recommendation{resource="memory"}
 */
@RestController
@RequestMapping("/api")
public class AlertEngineController {

    private final AlertRuleEvaluator evaluator;
    private final AlertRepository repository;
    private final DeviceStateManager stateManager;
    private final Counter            evaluatedCounter;
    private final Counter            firedCounter;

    public AlertEngineController(AlertRuleEvaluator evaluator,
                                 AlertRepository repository,
                                 DeviceStateManager stateManager,
                                 MeterRegistry registry) {
        this.evaluator        = evaluator;
        this.repository       = repository;
        this.stateManager     = stateManager;
        this.evaluatedCounter = registry.counter("alert.readings.evaluated");
        this.firedCounter     = registry.counter("alert.alerts.fired");
    }

    /**
     * Called by processor-service for every anomalous reading.
     * Returns 200 whether or not an alert was fired.
     */
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluate(@RequestBody ProcessedReading reading) {
        evaluatedCounter.increment();

        Optional<AlertDto> alert = evaluator.evaluate(reading);

        alert.ifPresent(a -> {
            firedCounter.increment();
            repository.save(toEntity(a));
        });

        return ResponseEntity.ok(Map.of(
                "evaluated", true,
                "alertFired", alert.isPresent()
        ));
    }

    /**
     * Health + stats endpoint — polled by Grafana.
     * estimatedMemoryBytes is the key metric for the VPA experiment:
     * as fleet grows, this number climbs and VPA should track it.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> stats() {
        return ResponseEntity.ok(Map.of(
                "trackedDevices",      stateManager.getTrackedDeviceCount(),
                "estimatedMemoryBytes",stateManager.estimatedMemoryBytes(),
                "alertsLast1h",        repository.countByTriggeredAtAfter(
                        Instant.now().minusSeconds(3600)),
                "status",              "running"
        ));
    }

    @GetMapping("/alerts/recent")
    public ResponseEntity<?> recentAlerts() {
        return ResponseEntity.ok(
                repository.findByTriggeredAtAfterOrderByTriggeredAtDesc(
                        Instant.now().minusSeconds(3600)
                )
        );
    }

    // ── Mapper ───────────────────────────────────────────────────────────

    private AlertEntity toEntity(AlertDto dto) {
        AlertEntity e = new AlertEntity();
        e.setDeviceId(dto.getDeviceId());
        // e.setNetworkType(dto.getNetworkType());
        e.setMessage(dto.getMessage());
        // e.setSeverity(dto.getSeverity());
        e.setZScore(dto.getZScore());
        e.setValue(dto.getValue());
        // e.setTriggeredAt(dto.getTriggeredAt());
        return e;
    }
}
