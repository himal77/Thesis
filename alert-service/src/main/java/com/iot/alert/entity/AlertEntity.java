
package com.iot.alert.entity;

import com.iot.commons.model.NetworkType;
import jakarta.persistence.*;
import lombok.*;

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