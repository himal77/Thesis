package com.iot.commons.dto;

import com.iot.commons.model.NetworkType;
import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Builder
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SensorReading {

    private String deviceId;
    private NetworkType networkType;
    private double value;
    private String unit;
    private long timestamp;

    private static final Map<NetworkType, List<String>> DEVICE_POOL =
            Arrays.stream(NetworkType.values())
                    .collect(Collectors.toMap(
                            type -> type,
                            type -> IntStream.range(0, 1000)
                                    .mapToObj(i -> type.name().toLowerCase() + "-" + i)
                                    .collect(Collectors.toList())
                    ));

    private static final Random rnd = new Random();

    public static SensorReading random(NetworkType type) {
        // pick a random device from the fixed pool — same IDs reused
        List<String> pool     = DEVICE_POOL.get(type);
        String       deviceId = pool.get(rnd.nextInt(pool.size()));

        return switch (type) {
            case TRAFFIC     -> new SensorReading(deviceId, type, rnd.nextInt(120), "km/h", System.currentTimeMillis());
            case ENVIRONMENT -> new SensorReading(deviceId, type,15 + rnd.nextDouble() * 25, "°C", System.currentTimeMillis());
            case ENERGY      -> new SensorReading(deviceId, type,rnd.nextDouble() * 10, "kWh", System.currentTimeMillis());
            case INDUSTRIAL  -> new SensorReading(deviceId, type,rnd.nextDouble() * 100, "mm/s", System.currentTimeMillis());
        };
    }
}
