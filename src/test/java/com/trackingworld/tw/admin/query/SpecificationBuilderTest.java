package com.trackingworld.tw.admin.query;

import com.trackingworld.tw.alerts.domain.Severity;
import com.trackingworld.tw.common.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpecificationBuilderTest {

    @Test
    void coercesValuesToAttributeTypes() {
        assertThat(SpecificationBuilder.coerce(Long.class, "42")).isEqualTo(42L);
        assertThat(SpecificationBuilder.coerce(Long.class, 42)).isEqualTo(42L);
        assertThat(SpecificationBuilder.coerce(Integer.class, "3")).isEqualTo(3);
        assertThat(SpecificationBuilder.coerce(Double.class, "24.5")).isEqualTo(24.5);
        assertThat(SpecificationBuilder.coerce(Boolean.class, "true")).isEqualTo(true);
        assertThat(SpecificationBuilder.coerce(Boolean.class, false)).isEqualTo(false);
        assertThat(SpecificationBuilder.coerce(Severity.class, "critical")).isEqualTo(Severity.CRITICAL);
        assertThat(SpecificationBuilder.coerce(Instant.class, "2026-10-05T05:12:00Z")).isEqualTo(Instant.parse("2026-10-05T05:12:00Z"));
        assertThat(SpecificationBuilder.coerce(Instant.class, "2026-10-05T10:12:00+05:00")).isEqualTo(Instant.parse("2026-10-05T05:12:00Z"));
        assertThat(SpecificationBuilder.coerce(Instant.class, "2026-10-05")).isEqualTo(Instant.parse("2026-10-05T00:00:00Z"));
        assertThat(SpecificationBuilder.coerce(String.class, "  abc ")).isEqualTo("abc");
    }

    @Test
    void rejectsBadValues() {
        assertThatThrownBy(() -> SpecificationBuilder.coerce(Long.class, "x")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SpecificationBuilder.coerce(Boolean.class, "yes")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SpecificationBuilder.coerce(Severity.class, "LOUD")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SpecificationBuilder.coerce(Instant.class, "yesterday")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> SpecificationBuilder.coerce(Long.class, null)).isInstanceOf(BadRequestException.class);
    }

    @Test
    void escapesLikeWildcards() {
        assertThat(SpecificationBuilder.escapeLike("50%_done\\")).isEqualTo("50\\%\\_done\\\\");
    }
}
