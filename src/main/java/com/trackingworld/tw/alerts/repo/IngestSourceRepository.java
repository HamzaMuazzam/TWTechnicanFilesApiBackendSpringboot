package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.IngestSource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface IngestSourceRepository extends JpaRepository<IngestSource, Long>, JpaSpecificationExecutor<IngestSource> {

    Optional<IngestSource> findByApiKeyHashAndActiveTrue(String apiKeyHash);

    Optional<IngestSource> findByName(String name);

    boolean existsByNameIgnoreCase(String name);

    @Modifying
    @Query("update IngestSource s set s.lastUsedAt = :now where s.id = :id")
    int touch(@Param("id") Long id, @Param("now") Instant now);
}
