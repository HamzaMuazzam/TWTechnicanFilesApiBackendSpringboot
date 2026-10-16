package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.Notification;
import com.trackingworld.tw.alerts.domain.NotificationStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long>, JpaSpecificationExecutor<Notification> {

    @EntityGraph(attributePaths = {"alert", "alert.alertType"})
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"alert", "alert.alertType", "user"})
    Optional<Notification> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"user"})
    List<Notification> findByAlertIdOrderByIdAsc(Long alertId);

    @EntityGraph(attributePaths = {"alert", "alert.alertType"})
    List<Notification> findTop20ByUserIdOrderByIdDesc(Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    @Modifying
    @Query("update Notification n set n.readAt = :now, n.status = :read where n.user.id = :userId and n.readAt is null and n.id in :ids")
    int markRead(@Param("userId") Long userId, @Param("ids") Collection<Long> ids, @Param("now") Instant now,
                 @Param("read") NotificationStatus read);

    @Modifying
    @Query("update Notification n set n.readAt = :now, n.status = :read where n.user.id = :userId and n.readAt is null")
    int markAllRead(@Param("userId") Long userId, @Param("now") Instant now, @Param("read") NotificationStatus read);

    @Modifying
    @Query("update Notification n set n.readAt = :now, n.status = :read where n.user.id = :userId and n.readAt is null and n.id <= :beforeId")
    int markAllReadBefore(@Param("userId") Long userId, @Param("beforeId") Long beforeId, @Param("now") Instant now,
                          @Param("read") NotificationStatus read);

    long countByStatus(NotificationStatus status);
}
