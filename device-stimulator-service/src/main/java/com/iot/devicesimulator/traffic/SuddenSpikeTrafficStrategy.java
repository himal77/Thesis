package com.iot.devicesimulator.traffic;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.config.SimulatorConfig;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@AllArgsConstructor
public class SuddenSpikeTrafficStrategy implements TrafficStrategy {

    private final SimulatorConfig simulatorConfig;

    @Override
    public List<SensorReading> getTrafficInBatch() {
        return getTrafficInBatch(20 * simulatorConfig.getDevicesPerNetwork());
    }

    @Override
    public TrafficProfile getType() {
        return TrafficProfile.SUDDEN_SPIKE;
    }
}
