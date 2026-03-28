package com.iot.devicesimulator.config;

import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.traffic.*;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Component
public class BeanFactory {
    private Map<TrafficProfile, TrafficStrategy> strategies = new HashMap<>();
    private final List<TrafficStrategy> trafficStrategies = new ArrayList<>();

    private final FleetGrowthTrafficStrategy fleetGrowthTraffic;
    private final GradualRampTrafficStrategy gradualRampTraffic;
    private final SawtoothTrafficStrategy sawtoothTraffic;
    private final BaseLineTrafficStrategy baseLineTraffic;
    private final SuddenSpikeTrafficStrategy suddenSpikeTraffic;
    private final HoldTrafficStrategy holdTraffic;

    public BeanFactory(FleetGrowthTrafficStrategy fleetGrowthTraffic, GradualRampTrafficStrategy gradualRampTraffic, SawtoothTrafficStrategy sawtoothTraffic, BaseLineTrafficStrategy baseLineTraffic, SuddenSpikeTrafficStrategy suddenSpikeTraffic, HoldTrafficStrategy holdTraffic) {
        this.fleetGrowthTraffic = fleetGrowthTraffic;
        this.gradualRampTraffic = gradualRampTraffic;
        this.sawtoothTraffic = sawtoothTraffic;
        this.baseLineTraffic = baseLineTraffic;
        this.suddenSpikeTraffic = suddenSpikeTraffic;
        this.holdTraffic = holdTraffic;

        trafficStrategies.add(fleetGrowthTraffic);
        trafficStrategies.add(gradualRampTraffic);
        trafficStrategies.add(sawtoothTraffic);
        trafficStrategies.add(baseLineTraffic);
        trafficStrategies.add(suddenSpikeTraffic);
        trafficStrategies.add(holdTraffic);

        registerTraffic();
    }

    public void registerTraffic() {
        this.strategies = trafficStrategies.stream()
                .collect(Collectors.toMap(
                        TrafficStrategy::getType,
                        Function.identity()
                ));
    }

    public TrafficStrategy get(TrafficProfile profile) {
        return strategies.get(profile);
    }
}
