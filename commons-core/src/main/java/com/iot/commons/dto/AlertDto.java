package com.iot.commons.dto;

import com.iot.commons.model.NetworkType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
public class AlertDto {
    private String deviceId, message, category;
    private NetworkType networkType;
    double zScore, value;
    Instant triggeredAt;
}
