package com.IoT.query.controller;

import com.IoT.query.respository.AlertQueryRepository;
import com.IoT.query.respository.ReadingQueryRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

/**
 * Single endpoint that returns the full system overview.
 * This is what the main Grafana dashboard polls every 5 seconds.
 *
 * --- Thesis relevance (HPA) ---
 * This endpoint does 4 DB queries per call.
 * Under concurrent Grafana polling it becomes CPU + I/O bound.
 * HPA scales this service when Grafana refresh rate is high
 * during peak experiment load — an interesting side effect to
 * document in the thesis.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final ReadingQueryRepository readingRepo;
    private final AlertQueryRepository alertRepo;

    public StatsController(ReadingQueryRepository readingRepo,
                           AlertQueryRepository alertRepo) {
        this.readingRepo = readingRepo;
        this.alertRepo   = alertRepo;
    }

    @GetMapping("/overview")
    public ResponseEntity<?> overview(
            @RequestParam(defaultValue = "60") int minutes) {

        Instant since = Instant.now().minus(minutes, ChronoUnit.MINUTES);

        long totalReadings = readingRepo.countByTimestampAfter(since);
        long anomalies     = readingRepo.countByAnomalyTrueAndTimestampAfter(since);
        long totalAlerts   = alertRepo.countByTriggeredAtAfter(since);
        long critAlerts    = alertRepo.countByCategoryAndTriggeredAtAfter("CRITICAL", since);
        Double avgZ        = readingRepo.avgZScoreSince(since);

        return ResponseEntity.ok(Map.of(
                "readings", Map.of(
                        "total",       totalReadings,
                        "anomalies",   anomalies,
                        "anomalyRate", totalReadings == 0 ? 0.0
                                : (double) anomalies / totalReadings
                ),
                "alerts", Map.of(
                        "total",    totalAlerts,
                        "critical", critAlerts
                ),
                "avgZScore",     avgZ != null ? avgZ : 0.0,
                "windowMinutes", minutes,
                "timestamp",     Instant.now().toString()
        ));
    }
}
