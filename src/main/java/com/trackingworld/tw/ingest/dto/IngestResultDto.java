package com.trackingworld.tw.ingest.dto;

import java.util.List;

/** Response item for one ingested alert. */
public record IngestResultDto(
        String id,
        Long alertId,
        String result,
        int configuredPhones,
        int notified,
        int discarded,
        List<DiscardedDto> discardedPhones
) {
    public static final String ACCEPTED = "ACCEPTED";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String NO_CONFIGURATION = "NO_CONFIGURATION";

    public record DiscardedDto(String phone, String reason) {
    }
}
