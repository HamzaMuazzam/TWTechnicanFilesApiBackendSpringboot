package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.DeliveryStatus;
import com.trackingworld.tw.alerts.domain.NotificationDelivery;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long>, JpaSpecificationExecutor<NotificationDelivery> {

    @EntityGraph(attributePaths = {"device"})
    List<NotificationDelivery> findByNotificationIdOrderByIdAsc(Long notificationId);

    @EntityGraph(attributePaths = {"device"})
    List<NotificationDelivery> findByNotificationIdIn(Collection<Long> notificationIds);

    Optional<NotificationDelivery> findByNotificationIdAndDeviceId(Long notificationId, Long deviceId);

    @Query("select d.id from NotificationDelivery d where d.notification.alert.id = :alertId and d.status = :status")
    List<Long> findIdsByAlertAndStatus(@Param("alertId") Long alertId, @Param("status") DeliveryStatus status);

    @Query("select d.id from NotificationDelivery d where d.notification.id = :notificationId and d.status = :status")
    List<Long> findIdsByNotificationAndStatus(@Param("notificationId") Long notificationId, @Param("status") DeliveryStatus status);

    @Query("select d.id from NotificationDelivery d where d.status in :statuses and d.nextAttemptAt <= :now order by d.nextAttemptAt asc")
    List<Long> findDueIds(@Param("statuses") Collection<DeliveryStatus> statuses, @Param("now") Instant now, Pageable pageable);

    @Query("select count(d) from NotificationDelivery d where d.status in :statuses and d.nextAttemptAt <= :now")
    long countDue(@Param("statuses") Collection<DeliveryStatus> statuses, @Param("now") Instant now);

    @Query("select d from NotificationDelivery d join fetch d.device dev join fetch d.notification n join fetch n.alert a join fetch a.alertType where d.id in :ids")
    List<NotificationDelivery> findForDispatch(@Param("ids") Collection<Long> ids);

    long countByStatus(DeliveryStatus status);
}
