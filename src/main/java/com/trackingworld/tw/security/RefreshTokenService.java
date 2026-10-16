package com.trackingworld.tw.security;

import com.trackingworld.tw.alerts.domain.RefreshToken;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.repo.RefreshTokenRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.UnauthorizedException;
import com.trackingworld.tw.common.util.Hashing;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/** Opaque refresh tokens (30 days); only the SHA-256 hash is stored, every use rotates the token. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final JwtProperties jwtProperties;

    /** Returns the plaintext token to hand to the client. */
    @Transactional
    public String issue(SubjectType type, Long subjectId, String deviceId) {
        String plain = Hashing.randomToken(48);
        repository.save(RefreshToken.builder()
                .subjectType(type)
                .subjectId(subjectId)
                .deviceId(deviceId)
                .tokenHash(Hashing.sha256Hex(plain))
                .expiresAt(Instant.now().plus(jwtProperties.refreshTtl()))
                .build());
        return plain;
    }

    /** Validates + revokes the presented token and returns its record (caller issues a new one). */
    @Transactional
    public RefreshToken consume(String plainToken, SubjectType expectedType) {
        if (plainToken == null || plainToken.isBlank()) {
            throw new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN);
        }
        RefreshToken token = repository.findByTokenHash(Hashing.sha256Hex(plainToken.trim()))
                .orElseThrow(() -> new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN));
        Instant now = Instant.now();
        if (token.getSubjectType() != expectedType || !token.isUsable(now)) {
            log.warn("[AUTH] refresh rejected type={} subject={} revoked={} expired={}", token.getSubjectType(),
                    token.getSubjectId(), token.getRevokedAt() != null, !token.getExpiresAt().isAfter(now));
            throw new UnauthorizedException(ErrorCodes.INVALID_REFRESH_TOKEN);
        }
        token.setRevokedAt(now);
        return token;
    }

    @Transactional
    public int revokeAll(SubjectType type, Long subjectId) {
        return repository.revokeAllForSubject(type, subjectId, Instant.now());
    }

    @Transactional
    public int revokeDevice(Long userId, String deviceId) {
        return repository.revokeForDevice(SubjectType.USER, userId, deviceId, Instant.now());
    }
}
