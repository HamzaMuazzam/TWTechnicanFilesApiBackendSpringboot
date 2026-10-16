package com.trackingworld.tw.alerts.repo;

import com.trackingworld.tw.alerts.domain.RefreshToken;
import com.trackingworld.tw.alerts.domain.SubjectType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.subjectType = :type and t.subjectId = :subjectId and t.revokedAt is null")
    int revokeAllForSubject(@Param("type") SubjectType type, @Param("subjectId") Long subjectId, @Param("now") Instant now);

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.subjectType = :type and t.subjectId = :subjectId and t.deviceId = :deviceId and t.revokedAt is null")
    int revokeForDevice(@Param("type") SubjectType type, @Param("subjectId") Long subjectId, @Param("deviceId") String deviceId, @Param("now") Instant now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :threshold or (t.revokedAt is not null and t.revokedAt < :threshold)")
    int purgeOlderThan(@Param("threshold") Instant threshold);
}
