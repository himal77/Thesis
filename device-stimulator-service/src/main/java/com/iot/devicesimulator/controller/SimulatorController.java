package com.iot.devicesimulator.controller;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.NetworkType;
import com.iot.devicesimulator.config.SimulatorConfig;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
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

    private static final Logger log = LoggerFactory.getLogger(SimulatorController.class);
    public static final String INGEST_API_PATH = "/api/ingest";

    private final RestTemplate restTemplate;
    private final SimulatorConfig config;
    private final Counter sentCounter;
    private final Counter errorCounter;

    public SimulatorController(RestTemplate restTemplate,
                               SimulatorConfig config,
                               MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.sentCounter = registry.counter("simulator.readings.sent");
        this.errorCounter = registry.counter("simulator.readings.errors");
    }

    @GetMapping("/status")
    public ResponseEntity<?> status() {
        return ResponseEntity.ok(Map.of("status", "running"));
    }

    @GetMapping("/stimulate")
    public void stimulate(@RequestParam int batchSize) {
        List<SensorReading> batch = getTrafficInBatch(batchSize);
        if(batch.isEmpty()) return;

        sentCounter.increment(batch.size());
        try {
            restTemplate.postForEntity(
                    config.getIngestorUrl() + INGEST_API_PATH,
                    batch,
                    Void.class
            );
            sentCounter.increment(batch.size());
            log.debug("Emitted {} readings [batch={}]", batch.size(), batch);
        } catch (RestClientException e) {
            errorCounter.increment();
            log.warn("Failed to send batch: {}", e.getMessage());
        }
    }

    List<SensorReading> getTrafficInBatch(int batchSizePerNetwork) {
        List<SensorReading> batch = new ArrayList<>();
        for (NetworkType type : NetworkType.values()) {
            int count = Math.max(1, batchSizePerNetwork);
            for (int i = 0; i < count; i++) {
                batch.add(SensorReading.random(type));
            }
        }
        return batch;
    }
}
