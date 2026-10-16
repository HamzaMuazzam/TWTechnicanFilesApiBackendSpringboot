package com.trackingworld.tw.ingest.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/** One S3 Configuration row: ID, PhoneNumber, UID, AlertType, DBDT, CrtBY. */
public record IngestConfigurationRequest(
        @JsonAlias({"ID", "sourceId"}) @Size(max = 200) String id,
        @JsonAlias({"PhoneNumber", "phone"}) @NotBlank @Size(max = 40) String phoneNumber,
        @JsonAlias({"UID"}) @NotBlank @Size(max = 200) String uid,
        @JsonAlias({"AlertType"}) @Size(max = 100) String alertType,
        @JsonAlias({"DBDT"}) OffsetDateTime dbdt,
        @JsonAlias({"CrtBY", "crtBY", "createdBy"}) @Size(max = 100) String crtBy
) {
}
