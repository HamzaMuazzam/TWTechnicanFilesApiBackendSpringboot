package com.trackingworld.tw.ingest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.Map;

/** One S3 Alert row: ID, UID, AlertType, Message, Lat, Lon, DBDT. Recipients come from the Configuration table. */
public record IngestAlertRequest(
        @JsonAlias({"ID", "sourceAlertId"}) @Size(max = 200) String id,
        @JsonAlias({"UID"}) @NotBlank @Size(max = 200) String uid,
        @JsonAlias({"AlertType"}) @NotBlank @Size(max = 100) String alertType,
        @JsonAlias({"Message"}) @Size(max = 2000) String message,
        @JsonAlias({"Lat"}) Double lat,
        @JsonAlias({"Lon"}) Double lon,
        @JsonAlias({"DBDT"}) OffsetDateTime dbdt,
        Map<String, Object> extra
) {
    public String sourceAlertId() {
        return id;
    }
}
