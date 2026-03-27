package com.iot.alert.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AlertEngineConfig {

    // How many consecutive anomalies before firing an alert
    @Value("${alert.consecutive-threshold:3}")
    private int consecutiveThreshold;

    // How many readings to keep in history per device
    // This is what drives memory growth — key for VPA experiment
    @Value("${alert.history.size:200}")
    private int historySize;

    // Cooldown between alerts for the same device (seconds)
    @Value("${alert.cooldown-seconds:60}")
    private int cooldownSeconds;

}
