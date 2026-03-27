package com.iot.devicesimulator.service;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.NetworkType;
import com.iot.devicesimulator.config.SimulatorConfig;
import com.iot.devicesimulator.traffic.FleetGrowth;
import com.iot.devicesimulator.traffic.RampTraffic;
import com.iot.devicesimulator.traffic.SawtoothTraffic;
import lombok.Getter;
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

import com.iot.commons.model.TrafficProfile;


@Component
public class DeviceSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);

    private final RestTemplate restTemplate;
    private final SimulatorConfig config;
    private final Counter sentCounter;
    private final Counter errorCounter;
    @Getter
    private final AtomicReference<TrafficProfile> trafficProfile;

    private final FleetGrowth fleetGrowth;
    private final RampTraffic rampTraffic;
    private final SawtoothTraffic sawtoothTraffic;

    public DeviceSimulator(RestTemplate restTemplate,
                           SimulatorConfig config,
                           MeterRegistry registry,
                           FleetGrowth fleetGrowth,
                           RampTraffic rampTraffic,
                           SawtoothTraffic sawtoothTraffic) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.trafficProfile = new AtomicReference<>(config.getTrafficProfile());
        this.sentCounter = registry.counter("simulator.readings.sent");
        this.errorCounter = registry.counter("simulator.readings.errors");
        this.fleetGrowth = fleetGrowth;
        this.rampTraffic = rampTraffic;
        this.sawtoothTraffic = sawtoothTraffic;
    }

    /**
     * Fires every 500ms. Batch size scales with burstMultiplier().
     * At IDLE: ~4 readings/tick  (8/s)
     * At CASCADE: ~80 readings/tick (160/s) with 100 devices/network
     */
    @Scheduled(fixedDelay = 500)
    public void emit() {
        List<SensorReading> batch = buildBatchForCurrentProfile();

        System.out.println("Traffic Type: " + trafficProfile.get());
        System.out.println("Batch size:"  + batch.size());


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

    private List<SensorReading> buildBatchForCurrentProfile() {
        return switch (trafficProfile.get()) {
            case GRADUAL_RAMP -> buildBatch(rampTraffic.currentMultiplier(), config.getDevicesPerNetwork());
            case SAWTOOTH -> buildBatch(sawtoothTraffic.currentMultiplier(), config.getDevicesPerNetwork());
            case FLEET_GROWTH -> buildBatch(1, fleetGrowth.currentDeviceCount());
            case BASELINE -> buildBatch(3, config.getDevicesPerNetwork());
            case SUDDEN_SPIKE,
                 CASCADE,
                 SUSTAINED_MAX -> buildBatch(20, config.getDevicesPerNetwork());
        };
    }

    private List<SensorReading> buildBatch(int multiplier, int devicesPerNetwork) {
        List<SensorReading> batch = new ArrayList<>();

        for (NetworkType type : NetworkType.values()) {
            // How many devices of this type report per tick.
            // Divide by 10 so at multiplier=1 only 10% of fleet reports
            // each tick — realistic: not all devices fire simultaneously.
            // Multiply by multiplier to simulate burst scenarios.
            int count = Math.max(1, (devicesPerNetwork / 10) * multiplier);

            for (int i = 0; i < count; i++) {
                batch.add(SensorReading.random(type));
            }
        }

        return batch;
    }

    public void setTrafficProfile(TrafficProfile trafficProfile) {
        this.trafficProfile.set(trafficProfile);
    }

}