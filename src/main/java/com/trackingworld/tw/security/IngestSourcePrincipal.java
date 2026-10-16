package com.trackingworld.tw.security;

/** Authenticated ingest source (X-Api-Key). */
public record IngestSourcePrincipal(Long id, String name) {
}
