package com.trackingworld.tw.alerts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "app_setting")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppSetting {

    public static final String RETENTION_DAYS = "retention_days";
    public static final String MAX_DEVICES_PER_USER = "max_devices_per_user";
    public static final String DISPATCH_MAX_ATTEMPTS = "dispatch_max_attempts";
    /** Comma-separated keywords; a new alert type code containing one of them defaults to CRITICAL / WARNING. */
    public static final String SEVERITY_KEYWORDS_CRITICAL = "severity_keywords_critical";
    public static final String SEVERITY_KEYWORDS_WARNING = "severity_keywords_warning";

    @Id
    @Column(name = "key", columnDefinition = "text")
    private String key;

    @Column(nullable = false, columnDefinition = "text")
    private String value;

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();
}
