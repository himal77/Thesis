package com.iot.alert.service;

import com.iot.alert.entity.DeviceState;
import com.iot.alert.config.AlertEngineConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
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

    private final List<DeviceState> deviceStateList = new ArrayList<>();
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
}
