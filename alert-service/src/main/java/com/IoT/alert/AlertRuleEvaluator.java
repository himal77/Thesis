package com.IoT.alert;

import com.IoT.commons.dto.AlertDto;
import com.IoT.commons.dto.ProcessedReading;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Evaluates whether a ProcessedReading should trigger an alert.
 *
 * Rules:
 *  1. Reading must be flagged as anomaly (|zScore| > 2.5)
 *  2. Device must have N consecutive anomalies (prevents noise spikes)
 *  3. Device must not be in cooldown (prevents alert flooding)
 */
@Component
public class AlertRuleEvaluator {

    private final AlertEngineConfig  config;
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
        DeviceState state = stateManager.getOrCreate(
                reading.getOriginal().getDeviceId()
        );

        // update history regardless
        state.addReading(reading.getNormalized());

        if (!reading.isAnomaly()) {
            // normal reading — reset consecutive counter
            state.resetConsecutiveAnomalies();
            return Optional.empty();
        }

        // anomalous reading — increment counter
        state.incrementConsecutiveAnomalies();

        // Rule 1: not enough consecutive anomalies yet
        if (state.getConsecutiveAnomalies() < config.getConsecutiveThreshold()) {
            return Optional.empty();
        }

        // Rule 2: device is in cooldown
        if (isInCooldown(state)) {
            return Optional.empty();
        }

        // All rules passed — fire alert
        state.setLastAlertAt(Instant.now());
        state.resetConsecutiveAnomalies();

        return Optional.of(buildAlert(reading, state));
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
                reading.getOriginal().getDeviceId(),
                config.getConsecutiveThreshold(),
                reading.getZScore()
        );

        return new AlertDto(
                reading.getOriginal().getDeviceId(),
                reading.getOriginal().getNetworkType().name(),
                message,
                reading.getCategory(),
                reading.getZScore(),
                reading.getOriginal().getValue()
        );
    }
}
