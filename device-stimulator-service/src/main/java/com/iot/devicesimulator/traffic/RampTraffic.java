package com.iot.devicesimulator.traffic;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class RampTraffic {

    // Ramps multiplier from 1 → maxMultiplier over rampDurationMs
    // Called every scheduler tick to get the current multiplier

    private Instant rampStart;
    private final long rampDurationMs = 1 * 60 * 1000L; // 10 minutes
    private final int maxMultiplier = 10;

    public void start() {
        this.rampStart = Instant.now();
    }

    public void reset() {
        this.rampStart = null;
    }

    public int currentMultiplier() {
        if (rampStart == null) return 1;

        long elapsed = Duration.between(rampStart, Instant.now()).toMillis();
        double progress = Math.min(1.0, (double) elapsed / rampDurationMs);

        // Linear ramp: 1 → 10
        return (int) Math.max(1, progress * maxMultiplier);
    }
}
