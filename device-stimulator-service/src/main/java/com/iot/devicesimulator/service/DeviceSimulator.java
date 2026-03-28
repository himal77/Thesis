package com.iot.devicesimulator.service;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.config.BeanFactory;
import com.iot.devicesimulator.config.SimulatorConfig;
import com.iot.devicesimulator.traffic.*;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


@Component
public class DeviceSimulator {

    private static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);
    public static final String INGEST_API_PATH = "/api/ingest";

    private final RestTemplate restTemplate;
    private final SimulatorConfig config;
    private final Counter sentCounter;
    private final Counter errorCounter;
    private final BeanFactory beanFactory;
    private AtomicReference<TrafficStrategy> trafficStrategyAR;

    @Getter
    private final AtomicReference<TrafficProfile> trafficProfile;

    public DeviceSimulator(RestTemplate restTemplate,
                           SimulatorConfig config,
                           MeterRegistry registry,
                           BeanFactory beanFactory) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.trafficProfile = new AtomicReference<>(config.getTrafficProfile());
        this.sentCounter = registry.counter("simulator.readings.sent");
        this.errorCounter = registry.counter("simulator.readings.errors");
        this.beanFactory = beanFactory;
        trafficStrategyAR = new AtomicReference<>(beanFactory.get(TrafficProfile.HOLD));
    }

    @Scheduled(fixedDelay = 500)
    public void emit() {
        List<SensorReading> batch = trafficStrategyAR.get().getTrafficInBatch();
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

    public void setTrafficProfile(TrafficProfile trafficProfile) {
        this.trafficProfile.set(trafficProfile);
        trafficStrategyAR.set(beanFactory.get(trafficProfile));
    }
}