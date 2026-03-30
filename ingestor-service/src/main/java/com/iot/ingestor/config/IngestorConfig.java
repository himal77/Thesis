package com.iot.ingestor.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Getter
@Configuration
public class IngestorConfig {

    // Processor service URL — overridden by K8s env var
    @Value("${processor.url:http://localhost:8082}")
    private String processorUrl;

    // Max batch size accepted per request
    // Requests exceeding this are rejected with 500
    @Value("${ingestor.max-batch-size:5000}")
    private int maxBatchSize;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}
