package com.iot.ingestor.service;

import com.iot.commons.dto.SensorReading;
import com.iot.ingestor.config.IngestorConfig;
import org.springframework.stereotype.Component;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * HTTP client that forwards validated readings to processor-service.
 *
 * --- Thesis relevance (HPA) ---
 * This is the fan-out point that makes ingestor I/O bound.
 *
 * The ingestor receives 1 batch (e.g. 400 readings) but makes
 * 400 individual HTTP calls to processor-service. Each call
 * blocks a Tomcat thread. Under burst load the thread pool fills
 * and CPU climbs — which is the signal HPA uses to scale out.
 *
 * This design is intentional for the thesis:
 *   - it creates measurable CPU pressure on the ingestor
 *   - it creates measurable CPU pressure on the processor
 *   - both services scale independently via their own HPA
 *
 * In production you would batch these calls or use Kafka.
 * For the thesis, direct HTTP makes the scaling behavior
 * more visible and easier to attribute.
 */
@Component
public class ProcessorClient {

    private static final Logger log = LoggerFactory.getLogger(ProcessorClient.class);

    private final RestTemplate   restTemplate;
    private final IngestorConfig config;
    private final IngestorMetrics metrics;

    public ProcessorClient(RestTemplate restTemplate,
                           IngestorConfig config,
                           IngestorMetrics metrics) {
        this.restTemplate = restTemplate;
        this.config       = config;
        this.metrics      = metrics;
    }

    /**
     * Forwards a single reading to processor-service.
     * Returns true if successful, false if processor unreachable.
     */
    public boolean forward(SensorReading reading) {
        try {
            metrics.forwardTimer.record(() ->
                    restTemplate.postForEntity(
                            config.getProcessorUrl() + "/api/process",
                            reading,
                            Void.class
                    )
            );
            metrics.forwardedCounter.increment();
            return true;

        } catch (RestClientException e) {
            metrics.failedCounter.increment();
            log.warn("Failed to forward reading [device={}]: {}",
                    reading.getDeviceId(), e.getMessage());
            return false;
        }
    }
}
