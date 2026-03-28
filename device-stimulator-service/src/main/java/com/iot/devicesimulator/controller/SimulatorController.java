package com.iot.devicesimulator.controller;

import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.config.TrafficRegistryFactory;
import com.iot.devicesimulator.service.DeviceSimulator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

/**
 * REST API to control the simulator at runtime.
 *
 * Used by experiment scripts to switch scenarios without pod restarts:
 *   curl -X POST http://simulator/api/scenario -d '{"scenario":"RUSH_HOUR"}'
 */
@RestController
@RequestMapping("/api")
public class SimulatorController {

    private final DeviceSimulator deviceSimulator;
    private final TrafficRegistryFactory trafficRegistryFactory;

    public SimulatorController(DeviceSimulator deviceSimulator,
                               TrafficRegistryFactory trafficRegistryFactory) {
        this.deviceSimulator = deviceSimulator;
        this.trafficRegistryFactory = trafficRegistryFactory;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "scenario", deviceSimulator.getTrafficProfile().get(),
                "status", "running"
        ));
    }

    @PostMapping("/profile")
    public ResponseEntity<?> setProfile(@RequestBody Map<String, String> body) {
        // validate profile name before doing anything
        TrafficProfile profile;
        try {
            profile = TrafficProfile.valueOf(body.get("profile").toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid profile. Valid values: " +
                            Arrays.toString(TrafficProfile.values())
            ));
        }

        trafficRegistryFactory.resetTimer();
        trafficRegistryFactory.startTimer();
        deviceSimulator.setTrafficProfile(profile);

        return ResponseEntity.ok(Map.of("profile", profile.name()));
    }
}
