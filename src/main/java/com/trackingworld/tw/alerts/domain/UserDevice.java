package com.trackingworld.tw.alerts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_device")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    /** App-generated UUID, stable per install. */
    @Column(name = "device_id", nullable = false, columnDefinition = "text")
    private String deviceId;

    @Column(name = "fcm_token", columnDefinition = "text")
    private String fcmToken;

    @Column(columnDefinition = "text")
    private String manufacturer;

    @Column(columnDefinition = "text")
    private String model;

    @Column(name = "os_version", columnDefinition = "text")
    private String osVersion;

    @Column(name = "app_version", columnDefinition = "text")
    private String appVersion;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "battery_opt_ignored")
    private Boolean batteryOptIgnored;

    @Column(name = "unused_restrictions_disabled")
    private Boolean unusedRestrictionsDisabled;

    @Column(name = "notifications_enabled")
    private Boolean notificationsEnabled;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
