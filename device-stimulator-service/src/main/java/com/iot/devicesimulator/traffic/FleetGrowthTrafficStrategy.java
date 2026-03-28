package com.iot.devicesimulator.traffic;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.TrafficProfile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class FleetGrowthTrafficStrategy implements TrafficStrategy {

    private final int initialDeviceCount = 100;
    private final int devicesPerStep = 100;
    private final int stepIntervalMinutes = 1;
    private final int maxDeviceCount = 1000;

    private Instant rampStart;

    public void start() {
        this.rampStart = Instant.now();
    }

    public void reset() {
        this.rampStart = null;
    }

    public int getDevicesPerNetwork() {
        if (rampStart == null) return initialDeviceCount;
        long elapsedMinutes = Duration.between(rampStart, Instant.now()).toMinutes();
        int completedSteps = (int) (elapsedMinutes / stepIntervalMinutes);
        int calculatedDevices = initialDeviceCount + completedSteps * devicesPerStep;
        return Math.min(calculatedDevices, maxDeviceCount);
    }

    @Override
    public List<SensorReading> getTrafficInBatch() {
        return getTrafficInBatch(getDevicesPerNetwork());
    }

    @Override
    public TrafficProfile getType() {
        return TrafficProfile.FLEET_GROWTH;
    }
}
