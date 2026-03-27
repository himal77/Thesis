package com.IoT.alert;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "alerts",
        indexes = {
                @Index(name = "idx_alert_device_time",
                        columnList = "deviceId, triggeredAt DESC"),
                @Index(name = "idx_alert_severity",
                        columnList = "severity, triggeredAt DESC")
        })
public class AlertEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long    id;
    private String  deviceId;
    private String  networkType;
    private String  message;
    private String  severity;
    private double  zScore;
    private double  value;
    private Instant triggeredAt;

    public AlertEntity() {}

    // Getters & Setters
    public Long    getId()           { return id; }
    public String  getDeviceId()     { return deviceId; }
    public void    setDeviceId(String d)     { this.deviceId = d; }
    public String  getNetworkType()  { return networkType; }
    public void    setNetworkType(String n)  { this.networkType = n; }
    public String  getMessage()      { return message; }
    public void    setMessage(String m)      { this.message = m; }
    public String  getSeverity()     { return severity; }
    public void    setSeverity(String s)     { this.severity = s; }
    public double  getZScore()       { return zScore; }
    public void    setZScore(double z)       { this.zScore = z; }
    public double  getValue()        { return value; }
    public void    setValue(double v)        { this.value = v; }
    public Instant getTriggeredAt()  { return triggeredAt; }
    public void    setTriggeredAt(Instant t) { this.triggeredAt = t; }
}
