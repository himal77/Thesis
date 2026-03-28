package com.iot.devicesimulator.traffic;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.config.SimulatorConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class SawtoothTrafficStrategy implements TrafficStrategy {

    private SimulatorConfig simulatorConfig;

    // Every cycleMs: spike for spikeMs, then idle for rest of cycle
    private final long cycleMs = 1 * 60 * 1000L;  // 5 min cycle
    private final long spikeMs = 60 * 1000L;  // 1 min spike
    private final int spikeMult = 10;
    private final int idleMult = 1;

    private Instant rampStart;

    public SawtoothTrafficStrategy(SimulatorConfig simulatorConfig) {
        this.simulatorConfig = simulatorConfig;
    }

    @Override
    public void start() {
        this.rampStart = Instant.now();
    }

    @Override
    public void reset() {
        this.rampStart = null;
    }

    public int getCurrentMultiplier() {
        if (rampStart == null) return idleMult;
        long posInCycle = Duration.between(rampStart, Instant.now()).toMillis() % cycleMs;
        return posInCycle < spikeMs ? spikeMult : idleMult;
    }

    public List<SensorReading> getTrafficInBatch() {
        return getTrafficInBatch(simulatorConfig.getDevicesPerNetwork() * getCurrentMultiplier());
    }

    @Override
    public TrafficProfile getType() {
        return TrafficProfile.SAWTOOTH;
    }
}
