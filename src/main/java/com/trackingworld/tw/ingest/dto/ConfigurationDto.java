package com.trackingworld.tw.ingest.dto;

import com.trackingworld.tw.alerts.domain.Configuration;

import java.time.Instant;

public record ConfigurationDto(Long id, String sourceId, String phoneNumber, String phoneE164, String uid, String alertType,
                               Instant dbdt, String crtBy, Instant createdAt, Instant updatedAt) {

    public static ConfigurationDto of(Configuration c) {
        return new ConfigurationDto(c.getId(), c.getSourceId(), c.getPhoneRaw(), c.getPhoneE164(), c.getUid(), c.getAlertType(),
                c.getDbdt(), c.getCrtBy(), c.getCreatedAt(), c.getUpdatedAt());
    }
}
