package com.iot.processor.service;

import com.iot.commons.dto.ProcessedReading;
import com.iot.processor.config.ProcessorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Forwards ProcessedReading to alert-engine.
 * Fire-and-forget — processor does not wait for alert evaluation result.
 * If alert-engine is down, the reading is still saved to PostgreSQL.
 */
@Component
public class AlertEngineClient {

    private static final Logger log = LoggerFactory.getLogger(AlertEngineClient.class);

    private final RestTemplate    restTemplate;
    private final ProcessorConfig config;

    public AlertEngineClient(RestTemplate restTemplate, ProcessorConfig config) {
        this.restTemplate = restTemplate;
        this.config       = config;
    }

    public void forward(ProcessedReading reading) {
        try {
            restTemplate.postForEntity(
                    config.getAlertEngineUrl() + "/api/evaluate",
                    reading,
                    Void.class
            );
        } catch (RestClientException e) {
            // non-fatal — log and continue
            log.warn("Alert-engine unreachable for device {}: {}",
                    reading.getSensorReading().getDeviceId(), e.getMessage());
        }
    }
}
