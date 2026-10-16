package com.trackingworld.tw.security;

import com.trackingworld.tw.common.util.Hashing;

/** Ingest API keys: {@code twk_<random>}; only the SHA-256 hash and an 8-char display prefix are stored. */
public final class ApiKeys {

    public static final String KEY_PREFIX = "twk_";
    public static final int DISPLAY_PREFIX_LENGTH = 8;

    private ApiKeys() {
    }

    public static String generate() {
        return KEY_PREFIX + Hashing.randomToken(30);
    }

    public static String hash(String apiKey) {
        return Hashing.sha256Hex(apiKey.trim());
    }

    public static String prefix(String apiKey) {
        String trimmed = apiKey.trim();
        return trimmed.length() <= DISPLAY_PREFIX_LENGTH ? trimmed : trimmed.substring(0, DISPLAY_PREFIX_LENGTH);
    }
}
