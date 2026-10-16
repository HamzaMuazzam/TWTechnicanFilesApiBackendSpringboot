package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {

    boolean existsBySourceIdAndSourceAlertId(Long sourceId, String sourceAlertId);

    Optional<Alert> findBySourceIdAndSourceAlertId(Long sourceId, String sourceAlertId);

    boolean existsByAlertTypeCode(String alertTypeCode);

    @Modifying
    @Query(value = "delete from alert where sdt < :cutoff", nativeQuery = true)
    int deleteOlderThan(@Param("cutoff") Instant cutoff);
}
