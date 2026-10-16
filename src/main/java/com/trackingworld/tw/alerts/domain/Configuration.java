package com.trackingworld.tw.alerts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** S3 Configuration row: phone number that receives alerts of a tracker (uid). alert_type '*' = all types. */
@Entity
@Table(name = "configuration")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Configuration {

    public static final String ALL_TYPES = "*";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_id", columnDefinition = "text")
    private String sourceId;

    @Column(name = "phone_raw", nullable = false, columnDefinition = "text")
    private String phoneRaw;

    @Column(name = "phone_e164", nullable = false, columnDefinition = "text")
    private String phoneE164;

    @Column(nullable = false, columnDefinition = "text")
    private String uid;

    @Column(name = "alert_type", nullable = false, columnDefinition = "text")
    private String alertType;

    private Instant dbdt;

    @Column(name = "crt_by", columnDefinition = "text")
    private String crtBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
