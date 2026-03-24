package com.iot.devicesimulator.service;

import com.iot.devicesimulator.config.SimulatorConfig;
import com.iot.devicesimulator.model.SensorReading;
import com.iot.devicesimulator.scenario.NetworkType;
import com.iot.devicesimulator.scenario.ScenarioType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import io.micrometer.core.instrument.Counter;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class DeviceSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);

    private final RestTemplate restTemplate;
    private final SimulatorConfig config;
    private final Counter sentCounter;
    private final Counter errorCounter;

    // Mutable at runtime via REST endpoint (see SimulatorController)
    private final AtomicReference<ScenarioType> currentScenario;

    public DeviceSimulator(RestTemplate restTemplate,
                           SimulatorConfig config,
                           MeterRegistry registry) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.currentScenario = new AtomicReference<>(config.getScenario());
        this.sentCounter  = registry.counter("simulator.readings.sent");
        this.errorCounter = registry.counter("simulator.readings.errors");
    }

    /**
     * Fires every 500ms. Batch size scales with burstMultiplier().
     * At IDLE: ~4 readings/tick  (8/s)
     * At CASCADE: ~80 readings/tick (160/s) with 100 devices/network
     */
    @Scheduled(fixedDelay = 500)
    public void emit() {
        ScenarioType scenario = currentScenario.get();
        int multiplier = burstMultiplier(scenario);
        List<SensorReading> batch = buildBatch(multiplier);
        System.out.println(batch);

        sentCounter.increment(batch.size());
        /*
        try {
            restTemplate.postForEntity(
                    config.getIngestorUrl() + "/api/ingest",
                    batch,
                    Void.class
            );
            sentCounter.increment(batch.size());
            log.debug("Emitted {} readings [scenario={}]", batch.size(), scenario);
        } catch (RestClientException e) {
            errorCounter.increment();
            log.warn("Failed to send batch: {}", e.getMessage());
        }
         */
    }

    /**
     * Builds one batch of readings spread across all 4 network types.
     * multiplier controls how many devices "wake up" per tick.
     */
    private List<SensorReading> buildBatch(int multiplier) {
        List<SensorReading> batch = new ArrayList<>();
        for (NetworkType type : NetworkType.values()) {
            int count = Math.max(1, (config.getDevicesPerNetwork() / 10) * multiplier);
            for (int i = 0; i < count; i++) {
                batch.add(SensorReading.random(type));
            }
        }
        return batch;
    }

    /**
     * Burst multiplier — the key experimental variable.
     *
     * IDLE         = 1x  (baseline, very low traffic)
     * RUSH_HOUR    = 10x (traffic + energy sensors spike)
     * STORM_EVENT  = 5x  (environment sensors flood)
     * SHIFT_CHANGE = 20x (industrial sensors burst hard)
     * CASCADE      = 30x (all networks simultaneously — worst case)
     */
    private int burstMultiplier(ScenarioType scenario) {
        return switch (scenario) {
            case IDLE         -> 1;
            case RUSH_HOUR    -> 10;
            case STORM_EVENT  -> 5;
            case SHIFT_CHANGE -> 20;
            case CASCADE      -> 30;
        };
    }

    public void setScenario(ScenarioType scenario) {
        log.info("Scenario changed: {} → {}", currentScenario.get(), scenario);
        currentScenario.set(scenario);
    }

    public ScenarioType getCurrentScenario() {
        return currentScenario.get();
    }
}