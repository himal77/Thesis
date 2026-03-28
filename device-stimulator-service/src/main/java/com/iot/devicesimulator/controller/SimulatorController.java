package com.iot.devicesimulator.controller;

import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.config.BeanFactory;
import com.iot.devicesimulator.service.DeviceSimulator;
import com.iot.devicesimulator.traffic.*;
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
    private final BeanFactory beanFactory;

    public SimulatorController(DeviceSimulator deviceSimulator,
                               BeanFactory beanFactory) {
        this.deviceSimulator = deviceSimulator;
        this.beanFactory = beanFactory;
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

        beanFactory.getGradualRampTraffic().reset();
        beanFactory.getSawtoothTraffic().reset();
        beanFactory.getFleetGrowthTraffic().reset();

        // start the right traffic generator
        switch (profile) {
            case GRADUAL_RAMP  -> beanFactory.getGradualRampTraffic().start();
            case SAWTOOTH      -> beanFactory.getSawtoothTraffic().start();
            case FLEET_GROWTH  -> beanFactory.getFleetGrowthTraffic().start();
            default            -> {}  // stateless profiles need no start()
        }

        deviceSimulator.setTrafficProfile(profile);
        return ResponseEntity.ok(Map.of("profile", profile.name()));
    }
}
