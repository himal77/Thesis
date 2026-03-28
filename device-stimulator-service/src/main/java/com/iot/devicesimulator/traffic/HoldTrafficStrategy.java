package com.iot.devicesimulator.traffic;

import com.iot.commons.dto.SensorReading;
import com.iot.commons.model.TrafficProfile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class HoldTrafficStrategy implements TrafficStrategy {
    @Override
    public List<SensorReading> getTrafficInBatch() {
        return new ArrayList<>();
    }

    @Override
    public TrafficProfile getType() {
        return TrafficProfile.HOLD;
    }
}
