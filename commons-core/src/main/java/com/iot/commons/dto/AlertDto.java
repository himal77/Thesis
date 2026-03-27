package com.iot.commons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlertDto {
    String deviceId, name, message, category;
    double zScore, value;
}
