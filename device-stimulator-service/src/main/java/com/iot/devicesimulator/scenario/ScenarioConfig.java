package com.iot.devicesimulator.scenario;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@ConfigurationProperties("scenario")
@Component
public class ScenarioConfig {
    private int devicesCount;
    private long intervalMs;
    private NetworkType networkType;
    private ScenarioType scenarioType;
}
