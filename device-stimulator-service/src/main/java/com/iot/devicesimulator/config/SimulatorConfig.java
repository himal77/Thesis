package com.iot.devicesimulator.config;

import com.iot.devicesimulator.scenario.ScenarioType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SimulatorConfig {

    @Value("${simulator.scenario:IDLE}")
    private ScenarioType scenario;

    @Value("${simulator.devices-per-network:100}")
    private int devicesPerNetwork;

    @Value("${ingestor.url:http://localhost:8081}")
    private String ingestorUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    public ScenarioType getScenario() { return scenario; }
    public int getDevicesPerNetwork() { return devicesPerNetwork; }
    public String getIngestorUrl() { return ingestorUrl; }
}
