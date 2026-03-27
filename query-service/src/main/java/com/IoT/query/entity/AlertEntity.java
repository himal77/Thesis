package com.IoT.query.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlertEntity {

    @Id
    private Long id;
    private String deviceId;
    private String networkType;
    private String message;
    private String severity;
    private double zScore;
    private double value;
    private Instant triggeredAt;
}
