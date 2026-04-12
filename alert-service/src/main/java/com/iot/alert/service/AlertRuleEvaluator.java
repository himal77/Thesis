package com.iot.alert.service;

import com.iot.alert.entity.DeviceState;
import com.iot.alert.config.AlertEngineConfig;
import com.iot.commons.dto.AlertDto;
import com.iot.commons.dto.ProcessedReading;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Evaluates whether a ProcessedReading should trigger an alert.
 *
 * Rules:
 *  1. Reading must be flagged as anomaly ()
 *  2. Device must have N consecutive anomalies (prevents noise spikes)
 *  3. Device must not be in cooldown (prevents alert flooding)
 */
@Component
public class AlertRuleEvaluator {

    private final AlertEngineConfig config;
    private final DeviceStateManager stateManager;

    public AlertRuleEvaluator(AlertEngineConfig config,
                              DeviceStateManager stateManager) {
        this.config       = config;
        this.stateManager = stateManager;
    }

    /**
     * Evaluates rules and returns an AlertDto if an alert should fire,
     * or empty if the reading is within normal parameters.
     */
    public Optional<AlertDto> evaluate(ProcessedReading reading) {
        DeviceState deviceState = stateManager.getOrCreate(
                reading.getSensorReading().getDeviceId()
        );

        // update history regardless
        deviceState.addReading(reading.getSensorReading().getValue());

        if (!reading.isAnomaly()) {
            // normal reading — reset consecutive counter
            deviceState.resetConsecutiveAnomalies();
            return Optional.empty();
        }

        // anomalous reading — increment counter
        deviceState.incrementConsecutiveAnomalies();

        // Rule 1: not enough consecutive anomalies yet
        if (deviceState.getConsecutiveAnomalies() < config.getConsecutiveThreshold()) {
            return Optional.empty();
        }

        // Rule 2: device is in cooldown
        if (isInCooldown(deviceState)) {
            return Optional.empty();
        }

        // All rules passed — fire alert
        deviceState.setLastAlertAt(Instant.now());
        deviceState.resetConsecutiveAnomalies();

        return Optional.of(buildAlert(reading, deviceState));
    }

    private boolean isInCooldown(DeviceState state) {
        if (state.getLastAlertAt() == null) return false;
        long secondsSinceLast = Duration.between(
                state.getLastAlertAt(), Instant.now()
        ).toSeconds();
        return secondsSinceLast < config.getCooldownSeconds();
    }

    private AlertDto buildAlert(ProcessedReading reading, DeviceState state) {
        String message = String.format(
                "Device %s reported %s readings with |zScore|=%.2f",
                reading.getSensorReading().getDeviceId(),
                config.getConsecutiveThreshold(),
                reading.getZScore()
        );

        return AlertDto.builder()
                .deviceId(reading.getSensorReading().getDeviceId())
                .networkType(reading.getSensorReading().getNetworkType())
                .message(message)
                .category(reading.getCategory())
                .zScore(reading.getZScore())
                .value(reading.getSensorReading().getValue())
                .triggeredAt(Instant.now())
                .build();
    }
}
