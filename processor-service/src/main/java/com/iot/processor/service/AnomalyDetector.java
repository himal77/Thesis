package com.iot.processor.service;

import com.iot.commons.dto.SensorReading;
import com.iot.processor.config.ProcessorConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes Z-score for each reading using a per-device sliding window.
 *
 * --- Thesis relevance (HPA) ---
 * This is the PRIMARY reason processor-service is CPU bound.
 * Every reading triggers:
 *   1. A ConcurrentHashMap lookup
 *   2. A window update (add + maybe evict oldest)
 *   3. Mean computation  → O(n) over windowSize
 *   4. StdDev computation → O(n) over windowSize
 *   5. Z-score division
 *
 * At CASCADE burst with 800 readings/tick at 500ms = 1600 readings/sec,
 * each requiring O(100) math → CPU saturates → HPA fires.
 *
 * windowSize=100 is the tuning knob:
 *   higher = more accurate anomaly detection, more CPU per reading
 *   lower  = less CPU, less accurate
 */
@Component
public class AnomalyDetector {

    // Per-device sliding window of recent values
    // Key: deviceId, Value: ring buffer of last N readings
    private final Map<String, Deque<Double>> windows = new ConcurrentHashMap<>();

    private final ProcessorConfig config;

    public AnomalyDetector(ProcessorConfig config) {
        this.config = config;
    }

    /**
     * Computes the Z-score of this reading against the device's history.
     * Adds the reading to the window after computing.
     *
     * Returns 0.0 if the device has fewer than 2 readings (no baseline yet).
     */
    public double computeZScore(SensorReading reading) {
        Deque<Double> window = windows.computeIfAbsent(
                reading.getDeviceId(),
                id -> new ArrayDeque<>(config.getWindowSize())
        );

        double zScore = 0.0;

        if (window.size() >= 2) {
            double mean   = computeMean(window);
            double stdDev = computeStdDev(window, mean);
            zScore = stdDev == 0 ? 0.0 : (reading.getValue() - mean) / stdDev;
        }

        // add to window, evict oldest if full
        if (window.size() >= config.getWindowSize()) {
            window.pollFirst();
        }
        window.addLast(reading.getValue());

        return zScore;
    }

    public int getTrackedDeviceCount() {
        return windows.size();
    }

    public void clearDevice(String deviceId) {
        windows.remove(deviceId);
    }

    // ── Private math ─────────────────────────────────────────────────────

    private double computeMean(Deque<Double> window) {
        double sum = 0.0;
        for (double v : window) sum += v;
        return sum / window.size();
    }

    private double computeStdDev(Deque<Double> window, double mean) {
        double sumSquaredDiffs = 0.0;
        for (double v : window) {
            double diff = v - mean;
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs / window.size());
    }
}
