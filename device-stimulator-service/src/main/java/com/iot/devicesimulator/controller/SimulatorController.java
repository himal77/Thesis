package com.iot.devicesimulator.controller;

import com.iot.devicesimulator.scenario.ScenarioType;
import com.iot.devicesimulator.service.DeviceSimulator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    public SimulatorController(DeviceSimulator simulator) {
        this.simulator = simulator;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> status() {
        return ResponseEntity.ok(Map.of(
                "scenario", simulator.getCurrentScenario().name(),
                "status", "running"
        ));
    }

    @PostMapping("/scenario")
    public ResponseEntity<Map<String, String>> setScenario(@RequestBody Map<String, String> body) {
        try {
            ScenarioType scenario = ScenarioType.valueOf(body.get("scenario").toUpperCase());
            simulator.setScenario(scenario);
            return ResponseEntity.ok(Map.of(
                    "scenario", scenario.name(),
                    "message", "Scenario updated successfully"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Unknown scenario. Valid: IDLE, RUSH_HOUR, STORM_EVENT, SHIFT_CHANGE, CASCADE"
            ));
        }
    }
}
