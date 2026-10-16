package com.trackingworld.tw.firebase;

/** Outcome of one message in a sendEach batch. */
public record FcmSendResult(Long deliveryId, boolean success, String messageId, Failure failure, String error) {

    public enum Failure {
        /** Token is dead (UNREGISTERED / invalid registration token): drop the token, do not retry. */
        INVALID_TOKEN,
        /** Anything else: retry with backoff. */
        RETRYABLE
    }

    public static FcmSendResult ok(Long deliveryId, String messageId) {
        return new FcmSendResult(deliveryId, true, messageId, null, null);
    }

    public static FcmSendResult invalidToken(Long deliveryId, String error) {
        return new FcmSendResult(deliveryId, false, null, Failure.INVALID_TOKEN, error);
    }

    public static FcmSendResult retryable(Long deliveryId, String error) {
        return new FcmSendResult(deliveryId, false, null, Failure.RETRYABLE, error);
    }
}
