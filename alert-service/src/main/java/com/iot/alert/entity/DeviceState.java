package com.iot.alert.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holds runtime state for a single device.
 *
 * (VPA) ---
 * One DeviceState is allocated per unique deviceId seen.
 * Each instance holds:
 *   - a history ring buffer  (~200 doubles = ~1.6KB per device)
 *   - consecutive anomaly counter
 *   - last alert timestamp
 *
 * At 100 devices  → ~160KB  (trivial)
 * At 1000 devices → ~1.6MB  (still small)
 * At 10000 devices → ~16MB  (VPA must start tracking this)
 *
 * The FLEET_GROWTH traffic profile grows deviceId count over time,
 * forcing this map to expand and memory to climb — exactly what
 * VPA's recommender watches and adjusts requests.memory for.
 */
@Setter
@Getter
@AllArgsConstructor
public class DeviceState {

    private final String        deviceId;
    private final Deque<Double> history;
    private final int           maxHistory;
    private int                 consecutiveAnomalies = 0;
    private Instant             lastAlertAt          = null;

    public DeviceState(String deviceId, int maxHistory) {
        this.deviceId   = deviceId;
        this.maxHistory = maxHistory;
        this.history    = new ArrayDeque<>(maxHistory);
    }

    public void addReading(double value) {
        if (history.size() >= maxHistory) history.pollFirst();
        history.addLast(value);
    }

    public void incrementConsecutiveAnomalies() { consecutiveAnomalies++; }
    public void resetConsecutiveAnomalies()     { consecutiveAnomalies = 0; }
}
