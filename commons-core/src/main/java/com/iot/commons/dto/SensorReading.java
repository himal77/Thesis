package com.iot.commons.dto;

import com.iot.commons.model.NetworkType;
import lombok.*;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SensorReading {

    private String deviceId;
    private double value;
    private long timestamp;
    private NetworkType networkType;

    public static SensorReading random(NetworkType networkType) {
        return SensorReading.builder()
                .deviceId(UUID.randomUUID().toString())
                .value(ThreadLocalRandom.current().nextDouble(0, 5))
                .timestamp(System.currentTimeMillis())
                .networkType(networkType)
                .build();
    }
}
