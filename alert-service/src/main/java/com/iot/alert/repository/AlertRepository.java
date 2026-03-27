package com.iot.alert.repository;

import com.iot.alert.entity.AlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<AlertEntity, Long> {

    List<AlertEntity> findByDeviceIdOrderByTriggeredAtDesc(String deviceId);

    List<AlertEntity> findByCategoryAndTriggeredAtAfter(String severity, Instant since);

    List<AlertEntity> findByTriggeredAtAfterOrderByTriggeredAtDesc(Instant since);

    long countByTriggeredAtAfter(Instant since);
}
