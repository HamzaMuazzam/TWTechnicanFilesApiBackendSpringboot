package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConfigurationRepository extends JpaRepository<Configuration, Long>, JpaSpecificationExecutor<Configuration> {

    Optional<Configuration> findBySourceId(String sourceId);

    List<Configuration> findByUidOrderByIdAsc(String uid);

    /** Phones configured for this uid: rows for the exact alert type plus rows marked '*' (all types). */
    @Query("select distinct c.phoneE164 from Configuration c where c.uid = :uid")
    List<String> findPhones(@Param("uid") String uid);
}
