package com.trackingworld.tw.security;

import com.trackingworld.tw.alerts.domain.AdminRole;
import com.trackingworld.tw.alerts.domain.SubjectType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * HS256 access tokens. Claims (SPEC §7.2): sub=<id>, typ=USER|ADMIN, role (admin), did (deviceId, user).
 */
@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_TYPE = "typ";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_DEVICE = "did";
    public static final String CLAIM_USERNAME = "uname";

    private final SecretKey key;
    private final JwtProperties properties;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public long accessTtlSeconds() {
        return properties.accessTtl().toSeconds();
    }

    public String issueUserToken(Long userId, String deviceId) {
        return build(userId, SubjectType.USER)
                .claim(CLAIM_DEVICE, deviceId)
                .compact();
    }

    public String issueAdminToken(Long adminId, String username, AdminRole role) {
        return build(adminId, SubjectType.ADMIN)
                .claim(CLAIM_ROLE, role.name())
                .claim(CLAIM_USERNAME, username)
                .compact();
    }

    private io.jsonwebtoken.JwtBuilder build(Long subjectId, SubjectType type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(subjectId))
                .claim(CLAIM_TYPE, type.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.accessTtl())))
                .signWith(key, Jwts.SIG.HS256);
    }

    /** Parses and verifies; throws {@link TokenException} with an error code on any failure. */
    public ParsedToken parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            String typ = claims.get(CLAIM_TYPE, String.class);
            if (typ == null) {
                throw new TokenException("INVALID_TOKEN");
            }
            SubjectType type = SubjectType.valueOf(typ);
            long subjectId = Long.parseLong(claims.getSubject());
            String role = claims.get(CLAIM_ROLE, String.class);
            return new ParsedToken(type, subjectId,
                    claims.get(CLAIM_DEVICE, String.class),
                    claims.get(CLAIM_USERNAME, String.class),
                    role == null ? null : AdminRole.valueOf(role));
        } catch (ExpiredJwtException e) {
            throw new TokenException("TOKEN_EXPIRED");
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenException("INVALID_TOKEN");
        }
    }

    public record ParsedToken(SubjectType type, long subjectId, String deviceId, String username, AdminRole role) {
    }

    public static class TokenException extends RuntimeException {
        public TokenException(String code) {
            super(code);
        }
    }
}
