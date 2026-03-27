package com.IoT.query.entity;

import com.iot.commons.model.NetworkType;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long    id;
    private String  deviceId;
    private NetworkType networkType;
    private String  message;
    private String  category;
    private double  zScore;
    private double  value;
    private Instant triggeredAt;
}
