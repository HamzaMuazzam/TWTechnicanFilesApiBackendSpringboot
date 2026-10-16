package com.trackingworld.tw.security;

import com.trackingworld.tw.alerts.domain.AdminRole;
import com.trackingworld.tw.alerts.domain.SubjectType;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-that-is-definitely-longer-than-32-chars";

    private final JwtService jwtService = new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(30)));

    @Test
    void userTokenRoundTrip() {
        String token = jwtService.issueUserToken(42L, "device-uuid-1");
        JwtService.ParsedToken parsed = jwtService.parse(token);
        assertThat(parsed.type()).isEqualTo(SubjectType.USER);
        assertThat(parsed.subjectId()).isEqualTo(42L);
        assertThat(parsed.deviceId()).isEqualTo("device-uuid-1");
        assertThat(parsed.role()).isNull();
        assertThat(jwtService.accessTtlSeconds()).isEqualTo(900);
    }

    @Test
    void adminTokenRoundTrip() {
        String token = jwtService.issueAdminToken(7L, "admin", AdminRole.VIEWER);
        JwtService.ParsedToken parsed = jwtService.parse(token);
        assertThat(parsed.type()).isEqualTo(SubjectType.ADMIN);
        assertThat(parsed.subjectId()).isEqualTo(7L);
        assertThat(parsed.username()).isEqualTo("admin");
        assertThat(parsed.role()).isEqualTo(AdminRole.VIEWER);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.issueUserToken(1L, "d");
        String tampered = token.substring(0, token.length() - 4) + "abcd";
        assertThatThrownBy(() -> jwtService.parse(tampered))
                .isInstanceOf(JwtService.TokenException.class)
                .hasMessage("INVALID_TOKEN");
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService(new JwtProperties("another-secret-another-secret-another-secret", Duration.ofMinutes(15), Duration.ofDays(30)));
        String token = other.issueUserToken(1L, "d");
        assertThatThrownBy(() -> jwtService.parse(token)).hasMessage("INVALID_TOKEN");
    }

    @Test
    void expiredTokenIsReportedAsExpired() {
        JwtService shortLived = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-60), Duration.ofDays(30)));
        String token = shortLived.issueUserToken(1L, "d");
        assertThatThrownBy(() -> jwtService.parse(token)).hasMessage("TOKEN_EXPIRED");
    }
}
