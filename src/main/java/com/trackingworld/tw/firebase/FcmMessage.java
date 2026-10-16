package com.trackingworld.tw.firebase;

import java.util.Map;

/** Data-only FCM message for one device token. */
public record FcmMessage(Long deliveryId, String token, Map<String, String> data) {
}
