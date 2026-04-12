package com.IoT.query.controller;

import com.IoT.query.config.QueryApiConfig;
import com.IoT.query.entity.ReadingEntity;
import com.IoT.query.respository.ReadingQueryRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * REST API for querying sensor readings.
 * Consumed by Grafana dashboards during all 5 experiments.
 *
 * (HPA) ---
 * This service is mixed I/O + CPU bound:
 *   - I/O: PostgreSQL queries, especially range scans
 *   - CPU: aggregation queries (avgZScore, counts)
 *
 * Under load (Grafana polling every 5s × multiple dashboards)
 * CPU climbs and HPA scales out replicas.
 *
 * The queryTimer metric shows exactly how long each query takes
 * under different scaling configurations — key for E5 comparison.
 */
@RestController
@RequestMapping("/api/readings")
public class ReadingQueryController {

    private final ReadingQueryRepository repository;
    private final QueryApiConfig config;
    private final Counter                queryCounter;
    private final Timer                  queryTimer;

    public ReadingQueryController(ReadingQueryRepository repository,
                                  QueryApiConfig config,
                                  MeterRegistry registry) {
        this.repository   = repository;
        this.config       = config;
        this.queryCounter = registry.counter("query.readings.requests");
        this.queryTimer   = registry.timer("query.readings.duration");
    }

    /**
     * Recent readings — Grafana "live feed" panel.
     * Default: last 60 minutes, max 1000 rows.
     */
    @GetMapping("/recent")
    public ResponseEntity<List<ReadingEntity>> recent(
            @RequestParam(defaultValue = "60") int minutes) {

        queryCounter.increment();
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        List<ReadingEntity> results = queryTimer.record(() ->
                repository.findByTimestampBetweenOrderByTimestampDesc(
                        since,
                        Instant.now(),
                        PageRequest.of(0, config.getMaxResults())
                )
        );

        return ResponseEntity.ok(results);
    }

    /**
     * Readings for a specific device — Grafana per-device panel.
     */
    @GetMapping("/device/{deviceId}")
    public ResponseEntity<List<ReadingEntity>> byDevice(
            @PathVariable String deviceId,
            @RequestParam(defaultValue = "60") int minutes) {

        queryCounter.increment();
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        return ResponseEntity.ok(
                repository.findByDeviceIdAndTimestampBetween(
                        deviceId, since, Instant.now()
                )
        );
    }

    /**
     * Throughput stats — Grafana "readings/sec" panel.
     * Used in all experiments to correlate traffic with scaling events.
     */
    @GetMapping("/stats")
    public ResponseEntity<?> stats(
            @RequestParam(defaultValue = "60") int minutes) {

        queryCounter.increment();
        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        long total    = repository.countByTimestampAfter(since);
        long anomalies= repository.countByAnomalyTrueAndTimestampAfter(since);
        Double avgZ   = repository.avgZScoreSince(since);

        return ResponseEntity.ok(Map.of(
                "totalReadings",   total,
                "anomalies",       anomalies,
                "anomalyRate",     total == 0 ? 0.0 : (double) anomalies / total,
                "avgZScore",       avgZ != null ? avgZ : 0.0,
                "windowMinutes",   minutes
        ));
    }
}
