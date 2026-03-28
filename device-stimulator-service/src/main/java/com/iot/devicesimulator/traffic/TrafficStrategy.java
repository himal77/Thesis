package com.iot.devicesimulator.traffic;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.NetworkType;
import com.iot.commons.model.TrafficProfile;

import java.util.ArrayList;
import java.util.List;

public interface TrafficStrategy {
    List<SensorReading> getTrafficInBatch();

    default List<SensorReading> getTrafficInBatch(int batchSizePerNetwork) {
        List<SensorReading> batch = new ArrayList<>();
        for (NetworkType type : NetworkType.values()) {
            int count = Math.max(1, batchSizePerNetwork);
            for (int i = 0; i < count; i++) {
                batch.add(SensorReading.random(type));
            }
        }
        return batch;
    }

    TrafficProfile getType();

    default void reset() {
    }

    default void start() {
    }
}
