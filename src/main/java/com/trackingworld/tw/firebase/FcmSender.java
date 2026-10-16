package com.trackingworld.tw.firebase;

import java.util.List;

public interface FcmSender {

    /** Max messages per FCM sendEach call. */
    int MAX_BATCH = 500;

    /** Sends every message; the result list has one entry per input, in order. Never throws for per-message errors. */
    List<FcmSendResult> sendEach(List<FcmMessage> messages);
}
