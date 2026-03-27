package com.IoT.query.respository;

import com.IoT.query.entity.AlertEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface AlertQueryRepository
        extends JpaRepository<AlertEntity, Long> {

    List<AlertEntity> findByTriggeredAtAfterOrderByTriggeredAtDesc(
            Instant since, Pageable pageable
    );

    List<AlertEntity> findByDeviceIdAndTriggeredAtAfter(
            String deviceId, Instant since
    );

    List<AlertEntity> findBySeverityAndTriggeredAtAfter(
            String severity, Instant since
    );

    long countByTriggeredAtAfter(Instant since);

    long countBySeverityAndTriggeredAtAfter(String severity, Instant since);
}
