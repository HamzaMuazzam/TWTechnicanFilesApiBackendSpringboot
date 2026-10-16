package com.trackingworld.tw.alerts.dispatch;

/** Published inside the ingest transaction; dispatched after commit. */
public record AlertAcceptedEvent(Long alertId) {
}
