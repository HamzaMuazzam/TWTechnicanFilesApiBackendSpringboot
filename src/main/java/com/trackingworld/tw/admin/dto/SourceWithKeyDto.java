package com.trackingworld.tw.admin.dto;

/** Returned once on create / rotate-key: the plaintext key is never shown again. */
public record SourceWithKeyDto(Rows.SourceRow source, String apiKey) {
}
