package com.iot.devicesimulator.traffic;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class FleetGrowth {

    // Grows active device count every stepMs
    private final long timeIntervalToGrowth = 1;
    private final int  startDevices = 100;
    private final int  stepSize     = 100;   // +100 devices per step
    private final double stepMs     = 60 * 1000 * timeIntervalToGrowth; // every 10 min
    private final int  maxDevices   = 1000;

    private Instant rampStart;

    public void start() { this.rampStart = Instant.now(); }

    public void reset() {
        this.rampStart = null;
    }

    public int currentDeviceCount() {
        if (rampStart == null) return startDevices;
        double steps   = Duration.between(rampStart, Instant.now()).toMillis() / stepMs;
        int  devices = (int) (startDevices + steps * stepSize);
        return Math.min(devices, maxDevices);
    }
}
