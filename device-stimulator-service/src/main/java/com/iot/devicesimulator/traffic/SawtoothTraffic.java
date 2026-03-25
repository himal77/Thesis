package com.iot.devicesimulator.traffic;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class SawtoothTraffic {

    // Every cycleMs: spike for spikeMs, then idle for rest of cycle
    private final long cycleMs    = 1 * 60 * 1000L;  // 5 min cycle
    private final long spikeMs    =     60 * 1000L;  // 1 min spike
    private final int  spikeMult  = 10;
    private final int  idleMult   = 1;

    private Instant rampStart;

    public void start() { this.rampStart = Instant.now(); }

    public void reset() {
        this.rampStart = null;
    }

    public int currentMultiplier() {
        if (rampStart == null) return idleMult;
        long posInCycle = Duration.between(rampStart, Instant.now()).toMillis() % cycleMs;
        return posInCycle < spikeMs ? spikeMult : idleMult;
    }
}
