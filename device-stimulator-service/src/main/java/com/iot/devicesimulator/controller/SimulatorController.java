package com.iot.devicesimulator.controller;

import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.service.DeviceSimulator;
import com.iot.devicesimulator.traffic.FleetGrowth;
import com.iot.devicesimulator.traffic.RampTraffic;
import com.iot.devicesimulator.traffic.SawtoothTraffic;
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

    private final DeviceSimulator simulator;
    private final FleetGrowth fleetGrowth;
    private final RampTraffic rampTraffic;
    private final SawtoothTraffic sawtoothTraffic;

    public SimulatorController(DeviceSimulator simulator,
                               RampTraffic rampTraffic,
                               SawtoothTraffic sawtoothTraffic,
                               FleetGrowth fleetGrowth) {
        this.simulator = simulator;
        this.fleetGrowth = fleetGrowth;
        this.rampTraffic = rampTraffic;
        this.sawtoothTraffic = sawtoothTraffic;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of(
                "scenario", simulator.getTrafficProfile().get(),
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

        rampTraffic.reset();
        sawtoothTraffic.reset();
        fleetGrowth.reset();

        // start the right traffic generator
        switch (profile) {
            case GRADUAL_RAMP  -> rampTraffic.start();
            case SAWTOOTH      -> sawtoothTraffic.start();
            case FLEET_GROWTH  -> fleetGrowth.start();
            default            -> {}  // stateless profiles need no start()
        }

        simulator.setTrafficProfile(profile);
        return ResponseEntity.ok(Map.of("profile", profile.name()));
    }
}
