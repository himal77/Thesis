package com.iot.alert;

import com.iot.alert.config.AlertEngineConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages in-memory state for every device seen by the alert engine.
 *
 * --- Thesis relevance (VPA) ---
 * This ConcurrentHashMap is the reason VPA is the right scaler here.
 *
 * Memory usage grows with fleet size, NOT with request rate.
 * If you doubled the RPS but kept the same 100 devices, memory
 * would not grow. HPA watching CPU would scale out replicas —
 * but each replica would need the SAME amount of memory for its
 * share of the device fleet. VPA right-sizes that memory over time
 * as the fleet grows, preventing OOMKills.
 *
 * This is the central argument of the VPA chapter in the thesis.
 */
@Component
public class DeviceStateManager {

    private final Map<String, DeviceState> states = new ConcurrentHashMap<>();
    private final AlertEngineConfig config;

    public DeviceStateManager(AlertEngineConfig config) {
        this.config = config;
    }

    public DeviceState getOrCreate(String deviceId) {
        return states.computeIfAbsent(
                deviceId,
                id -> new DeviceState(id, config.getHistorySize())
        );
    }

    public int getTrackedDeviceCount() {
        return states.size();
    }

    // Estimated memory footprint in bytes
    // Used by /api/stats endpoint — visible in Grafana during VPA experiment
    public long estimatedMemoryBytes() {
        // each DeviceState ≈ historySize * 8 bytes (doubles) + overhead
        return (long) states.size() * config.getHistorySize() * 8;
    }

    public void remove(String deviceId) {
        states.remove(deviceId);
    }

    public void clear() {
        states.clear();
    }
}
