package com.iot.devicesimulator.model;

import com.iot.devicesimulator.scenario.NetworkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SensorReading {

    private String deviceId;
    private double value;
    private long timestamp;
    private NetworkType networkType;

    public static SensorReading random(NetworkType networkType) {
        return SensorReading.builder()
                .deviceId(UUID.randomUUID().toString())
                .value(ThreadLocalRandom.current().nextDouble(0, 100))
                .timestamp(System.currentTimeMillis())
                .networkType(networkType)
                .build();
    }
}
