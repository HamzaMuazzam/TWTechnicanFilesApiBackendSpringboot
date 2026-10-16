package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.UserDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long>, JpaSpecificationExecutor<UserDevice> {

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    List<UserDevice> findByUserIdOrderByCreatedAtAsc(Long userId);

    List<UserDevice> findByUserIdAndActiveTrueOrderByLastSeenAtAsc(Long userId);

    @Query("select d from UserDevice d where d.user.id = :userId and d.active = true and d.fcmToken is not null")
    List<UserDevice> findActiveWithToken(@Param("userId") Long userId);

    long countByActiveTrueAndFcmTokenIsNotNull();

    @Query("select count(d) from UserDevice d where d.active = true and (d.lastSeenAt is null or d.lastSeenAt < :threshold)")
    long countSilent(@Param("threshold") Instant threshold);

    @Modifying
    @Query("update UserDevice d set d.active = false, d.updatedAt = :now where d.user.id = :userId")
    int deactivateAllForUser(@Param("userId") Long userId, @Param("now") Instant now);

    /** Another device of the same user may already hold this token (reinstall) - clear it there. */
    @Modifying
    @Query("update UserDevice d set d.fcmToken = null, d.updatedAt = :now where d.fcmToken = :token and d.id <> :keepId")
    int clearTokenElsewhere(@Param("token") String token, @Param("keepId") Long keepId, @Param("now") Instant now);
}
