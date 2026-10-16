package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.DiscardedRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface DiscardedRecipientRepository extends JpaRepository<DiscardedRecipient, Long>, JpaSpecificationExecutor<DiscardedRecipient> {

    List<DiscardedRecipient> findByAlertIdOrderByIdAsc(Long alertId);
}
