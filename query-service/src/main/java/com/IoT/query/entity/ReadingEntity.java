package com.IoT.query.entity;

import com.iot.commons.model.NetworkType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
public class ReadingEntity {

    @Id
    private Long id;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private NetworkType networkType;

    private double value;
    private double zScore;
    private double normalized;
    private String category;
    private boolean anomaly;
    private Instant timestamp;
    private Instant processedAt;
}
