package com.iot.devicesimulator.config;

import com.iot.commons.model.TrafficProfile;
import com.iot.devicesimulator.traffic.TrafficStrategy;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Component
public class TrafficRegistryFactory {
    private Map<TrafficProfile, TrafficStrategy> strategies = new HashMap<>();

    public TrafficRegistryFactory(List<TrafficStrategy> trafficStrategyList) {
        registerTraffic(trafficStrategyList);
    }

    public void registerTraffic(List<TrafficStrategy> trafficStrategies) {
        this.strategies = trafficStrategies.stream()
                .collect(Collectors.toMap(
                        TrafficStrategy::getType,
                        Function.identity()
                ));
    }

    public TrafficStrategy get(TrafficProfile profile) {
        return strategies.get(profile);
    }

    public void resetTimer() {
        strategies.values().forEach(TrafficStrategy::reset);
    }

    public void startTimer() {
        strategies.values().forEach(TrafficStrategy::start);
    }
}