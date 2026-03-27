package com.iot.processor.service;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.NetworkType;
import org.springframework.stereotype.Component;

/**
 * Scales raw sensor values to 0.0–1.0 based on known ranges per network type.
 * Clamped so values outside the expected range don't exceed 0.0/1.0.
 */
@Component
public class ReadingNormalizer {

    public double normalize(SensorReading reading) {
        double min = minFor(reading.getNetworkType());
        double max = maxFor(reading.getNetworkType());
        double clamped = Math.max(min, Math.min(max, reading.getValue()));
        return (clamped - min) / (max - min);
    }

    private double minFor(NetworkType type) {
        return switch (type) {
            case TRAFFIC     -> 0.0;     // 0 km/h
            case ENVIRONMENT -> -20.0;   // -20°C
            case ENERGY      -> 0.0;     // 0 kWh
            case INDUSTRIAL  -> 0.0;     // 0 mm/s
        };
    }

    private double maxFor(NetworkType type) {
        return switch (type) {
            case TRAFFIC     -> 200.0;   // 200 km/h
            case ENVIRONMENT -> 50.0;    // 50°C
            case ENERGY      -> 20.0;    // 20 kWh
            case INDUSTRIAL  -> 200.0;   // 200 mm/s
        };
    }
}
