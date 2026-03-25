package com.iot.devicesimulator.config;

import com.IoT.commons.model.TrafficProfile;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class SimulatorConfig {

    @Value("${simulator.devices-per-network:100}")
    private int devicesPerNetwork;

    @Value("${simulator.current-traffic-profile:GRADUAL_RAMP}")
    private TrafficProfile trafficProfile;

    @Value("${ingestor.url:http://localhost:8081}")
    private String ingestorUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
