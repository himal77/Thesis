package com.iot.processor.entity;

import com.iot.commons.model.NetworkType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReadingEntity {

    // Getters & Setters
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String deviceId;

    @Enumerated(EnumType.STRING)
    private NetworkType networkType;

    private double value;
    private double zScore;
    private String category;
    private boolean anomaly;
    private Instant timestamp;
    private Instant processedAt;
}
