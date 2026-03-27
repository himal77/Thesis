package com.IoT.query.controller;

import com.IoT.query.config.QueryApiConfig;
import com.IoT.query.entity.AlertEntity;
import com.IoT.query.respository.AlertQueryRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * REST API for querying fired alerts.
 * Used by Grafana alert panels and experiment analysis scripts.
 */
@RestController
@RequestMapping("/api/alerts")
public class AlertQueryController {

    private final AlertQueryRepository repository;
    private final QueryApiConfig config;
    private final Counter              queryCounter;

    public AlertQueryController(AlertQueryRepository repository,
                                QueryApiConfig config,
                                MeterRegistry registry) {
        this.repository   = repository;
        this.config       = config;
        this.queryCounter = registry.counter("query.alerts.requests");
    }

    /**
     * Recent alerts — Grafana alert feed panel.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<AlertEntity>> recent(
            @RequestParam(defaultValue = "60") int minutes) {

        queryCounter.increment();
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        return ResponseEntity.ok(
                repository.findByTriggeredAtAfterOrderByTriggeredAtDesc(
                        since,
                        PageRequest.of(0, config.getMaxResults())
                )
        );
    }

    /**
     * Alerts for a specific device.
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<AlertEntity>> byDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "60") int minutes) {

        queryCounter.increment();
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        return ResponseEntity.ok(
                repository.findByDeviceIdAndTriggeredAtAfter(deviceId, since)
        );
    }

    /**
     * Alert counts by severity — Grafana severity breakdown panel.
     * Key during E4 (cascade) to show how many CRITICAL alerts fire.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> stats(
            @RequestParam(defaultValue = "60") int minutes) {

        queryCounter.increment();
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        return ResponseEntity.ok(Map.of(
                "total",    repository.countByTriggeredAtAfter(since),
                "critical", repository.countBySeverityAndTriggeredAtAfter("CRITICAL", since),
                "high",     repository.countBySeverityAndTriggeredAtAfter("HIGH", since),
                "medium",   repository.countBySeverityAndTriggeredAtAfter("MEDIUM", since),
                "low",      repository.countBySeverityAndTriggeredAtAfter("LOW", since),
                "windowMinutes", minutes
        ));
    }
}