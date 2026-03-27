package com.IoT.query.respository;

import com.IoT.query.entity.ReadingEntity;
import com.iot.commons.model.NetworkType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReadingQueryRepository
        extends JpaRepository<ReadingEntity, Long> {

    // E1 — used to plot reading throughput over time
    List<ReadingEntity> findByTimestampBetweenOrderByTimestampDesc(
            Instant from, Instant to, Pageable pageable
    );

    // E2 — used to track anomaly rate during fleet growth
    List<ReadingEntity> findByDeviceIdAndTimestampBetween(
            String deviceId, Instant from, Instant to
    );

    // Grafana: readings per network type over time
    List<ReadingEntity> findByNetworkTypeAndTimestampAfter(
            NetworkType networkType, Instant timestamp
    );

    // Grafana: anomaly rate — ratio of anomalies to total
    long countByTimestampAfter(Instant since);
    long countByAnomalyTrueAndTimestampAfter(Instant since);

    // E5 — throughput at different HPA thresholds
    @Query("SELECT COUNT(r) FROM ReadingEntity r " +
            "WHERE r.timestamp BETWEEN :from AND :to")
    long countBetween(@Param("from") Instant from,
                      @Param("to")   Instant to);

    // Grafana: average Z-score over time window
    @Query("SELECT AVG(r.zScore) FROM ReadingEntity r " +
            "WHERE r.timestamp > :since")
    Double avgZScoreSince(@Param("since") Instant since);
}
