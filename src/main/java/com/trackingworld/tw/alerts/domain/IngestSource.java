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

@Entity
@Table(name = "ingest_source")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, columnDefinition = "text")
    private String name;

    @Column(name = "api_key_hash", nullable = false, columnDefinition = "text")
    private String apiKeyHash;

    /** First 8 chars of the key, for display only. */
    @Column(name = "api_key_prefix", nullable = false, columnDefinition = "text")
    private String apiKeyPrefix;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "last_used_at")
    private Instant lastUsedAt;
}
