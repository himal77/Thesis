package com.iot.devicesimulator.traffic;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.config.SimulatorConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class GradualRampTrafficStrategy implements TrafficStrategy {

    private Instant rampStartTime;
    private final long rampDurationSeconds = 10 * 60; // 10 minutes
    private final int maxMultiplier = 10;

    private final SimulatorConfig simulatorConfig;

    public GradualRampTrafficStrategy(SimulatorConfig simulatorConfig) {
        this.simulatorConfig = simulatorConfig;
    }

    @Override
    public void start() {
        this.rampStartTime = Instant.now();
    }

    @Override
    public void reset() {
        this.rampStartTime = null;
    }

    public int getCurrentMultiplier() {
        if (rampStartTime == null) return 1;
        long elapsedSeconds = Duration.between(rampStartTime, Instant.now()).toSeconds();
        double rampProgress = Math.min(
                1.0,
                elapsedSeconds / (double) rampDurationSeconds
        );
        int multiplier = 1 + (int) (rampProgress * (maxMultiplier - 1));
        return Math.min(multiplier, maxMultiplier);
    }

    public List<SensorReading> getTrafficInBatch() {
        return getTrafficInBatch(simulatorConfig.getDevicesPerNetwork() * getCurrentMultiplier());
    }

    @Override
    public TrafficProfile getType() {
        return TrafficProfile.GRADUAL_RAMP;
    }
}
