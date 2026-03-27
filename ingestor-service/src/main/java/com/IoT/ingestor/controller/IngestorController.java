package com.IoT.ingestor.controller;

import com.IoT.ingestor.ProcessorClient;
import com.iot.commons.dto.SensorReading;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingest")
public class IngestorController {

    private final ProcessorClient processorClient;
    private final MeterRegistry registry;

    public IngestorController(ProcessorClient processorClient, MeterRegistry registry) {
        this.processorClient = processorClient;
        this.registry = registry;
    }

    @PostMapping
    public ResponseEntity<Void> ingest(@RequestBody List<SensorReading> readings) {
        readings.forEach(r -> {
            validate(r);                         // schema check
            processorClient.forward(r);          // HTTP call to processor
            registry.counter("readings.ingested").increment();
        });
        return ResponseEntity.accepted().build();
    }

    private void validate(SensorReading r) {
        if (r.getValue() == 0 || r.getDeviceId() == null)
            throw new IllegalArgumentException();
    }
}
