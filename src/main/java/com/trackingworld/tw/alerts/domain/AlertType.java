package com.trackingworld.tw.alerts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "alert_type")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertType {

    @Id
    @Column(columnDefinition = "text")
    private String code;

    @Column(nullable = false, columnDefinition = "text")
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "text")
    @Builder.Default
    private Severity severity = Severity.INFO;

    @Column(columnDefinition = "text")
    private String icon;

    @Column(columnDefinition = "text")
    private String color;

    @Column(name = "channel_id", columnDefinition = "text")
    private String channelId;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
