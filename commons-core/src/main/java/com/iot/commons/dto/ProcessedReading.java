package com.iot.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Output of processor-service.
 * Sent to alert-engine for state evaluation.
 * Also written to PostgreSQL for historical queries.
 * <p>
 * --- Thesis relevance ---
 * zScore is the CPU-heavy computation that makes processor-service
 * CPU bound. Computing it across a sliding window of 100 readings
 * per device per tick is what drives HPA to scale out replicas.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProcessedReading {

    private SensorReading sensorReading;
    private double zScore;       // how many std devs from mean
    private String category;     // LOW / MEDIUM / HIGH / CRITICAL
    private boolean anomaly;      // true if |zScore| > 2.5
    private Instant processedAt;
}
