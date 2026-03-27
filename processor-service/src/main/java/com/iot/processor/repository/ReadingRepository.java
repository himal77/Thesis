package com.iot.processor.repository;

import com.iot.commons.dto.ProcessedReading;
import com.iot.commons.model.NetworkType;
import com.iot.processor.entity.ReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ReadingRepository extends JpaRepository<ReadingEntity, Long> {

    List<ReadingEntity> findByDeviceIdAndTimestampBetween(
            String deviceId, Instant from, Instant to
    );

    List<ReadingEntity> findByAnomalyTrueAndTimestampAfter(Instant since);

    @Query("SELECT r FROM ReadingEntity r WHERE r.networkType = :networkType " +
            "AND r.timestamp > :since ORDER BY r.timestamp DESC")
    List<ReadingEntity> findRecentByNetwork(String networkType, Instant since);

    long countByAnomalyTrueAndTimestampAfter(Instant since);

    ProcessedReading getAllByNetworkType(NetworkType networkType);

    List<ReadingEntity> getByDeviceId(String deviceId);
}
