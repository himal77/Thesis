package com.iot.processor.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class ProcessorConfig {

    @Value("${alert-engine.url:http://localhost:8083}")
    private String alertEngineUrl;

    // Z-score threshold — reading is anomalous if |zScore| exceeds this
    @Value("${processor.anomaly.threshold:2.0}")
    private double anomalyThreshold;

    // How many past readings per device to keep in the sliding window
    @Value("${processor.window.size:100}")
    private int windowSize;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
