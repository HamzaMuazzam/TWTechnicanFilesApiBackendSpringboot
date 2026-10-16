package com.trackingworld.tw.alerts.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "alert")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_id", nullable = false)
    private IngestSource source;

    @Column(name = "source_alert_id", nullable = false, columnDefinition = "text")
    private String sourceAlertId;

    @Column(nullable = false, columnDefinition = "text")
    private String uid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "alert_type_code", nullable = false)
    private AlertType alertType;

    /** Read-only mirror of the FK column, handy for filtering/sorting without a join. */
    @Column(name = "alert_type_code", insertable = false, updatable = false, columnDefinition = "text")
    private String alertTypeCode;

    @Column(columnDefinition = "text")
    private String message;

    private Double lat;

    private Double lon;

    /** S3's own timestamp, stored as sent. */
    private Instant dbdt;

    /** When OUR server received it. */
    @Column(nullable = false)
    @Builder.Default
    private Instant sdt = Instant.now();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> extra;

    @Column(name = "recipients_total", nullable = false)
    private int recipientsTotal;

    @Column(name = "recipients_accepted", nullable = false)
    private int recipientsAccepted;

    @Column(name = "recipients_discarded", nullable = false)
    private int recipientsDiscarded;
}
