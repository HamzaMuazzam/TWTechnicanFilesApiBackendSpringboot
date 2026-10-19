package com.trackingworld.tw.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.SendResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Production sender: data-only messages, Android priority HIGH, TTL 24h, FirebaseMessaging.sendEach. */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
public class RealFcmSender implements FcmSender {

    private static final long TTL_MILLIS = Duration.ofHours(24).toMillis();

    private final FirebaseMessaging messaging;

    public RealFcmSender(FirebaseApp firebaseApp) {
        this.messaging = FirebaseMessaging.getInstance(firebaseApp);
    }

    @Override
    public List<FcmSendResult> sendEach(List<FcmMessage> messages) {
        List<FcmSendResult> results = new ArrayList<>(messages.size());
        for (int from = 0; from < messages.size(); from += MAX_BATCH) {
            List<FcmMessage> chunk = messages.subList(from, Math.min(messages.size(), from + MAX_BATCH));
            results.addAll(sendChunk(chunk));
        }
        return results;
    }

    private List<FcmSendResult> sendChunk(List<FcmMessage> chunk) {
        List<Message> payloads = chunk.stream().map(this::toMessage).toList();
        BatchResponse batch;
        try {
            batch = messaging.sendEach(payloads);
        } catch (FirebaseMessagingException e) {
            log.error("[FCM] sendEach failed for {} messages: {} {}", chunk.size(), e.getMessagingErrorCode(), e.getMessage());
            String error = "sendEach: " + e.getMessage();
            return chunk.stream().map(m -> FcmSendResult.retryable(m.deliveryId(), error)).toList();
        }
        List<FcmSendResult> out = new ArrayList<>(chunk.size());
        List<SendResponse> responses = batch.getResponses();
        for (int i = 0; i < chunk.size(); i++) {
            FcmMessage message = chunk.get(i);
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                out.add(FcmSendResult.ok(message.deliveryId(), response.getMessageId()));
            } else {
                FcmSendResult failure = map(message.deliveryId(), response.getException());
                log.warn("[FCM] delivery={} failed: {}", message.deliveryId(), failure.error());
                out.add(failure);
            }
        }
        log.info("[FCM] sendEach size={} success={} failure={}", chunk.size(), batch.getSuccessCount(), batch.getFailureCount());
        return out;
    }

    /**
     * firebase-admin 9.11 marks setToken deprecated in favour of setFid (Firebase Installation IDs). The app
     * registers classic FCM registration tokens (FirebaseMessaging.getToken()), which the FCM v1 API still
     * targets through the {@code token} field, so we keep using it deliberately.
     */
    @SuppressWarnings("deprecation")
    private Message toMessage(FcmMessage message) {
        return Message.builder()
                .setToken(message.token())
                .putAllData(message.data())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .setTtl(TTL_MILLIS)
                        .build())
                .build();
    }

    static FcmSendResult map(Long deliveryId, FirebaseMessagingException exception) {
        if (exception == null) {
            return FcmSendResult.retryable(deliveryId, "unknown FCM failure");
        }
        MessagingErrorCode code = exception.getMessagingErrorCode();
        String detail = (code != null ? code.name() : "UNKNOWN") + ": " + exception.getMessage();
        if (code == MessagingErrorCode.UNREGISTERED) {
            return FcmSendResult.invalidToken(deliveryId, detail);
        }
        if (code == MessagingErrorCode.INVALID_ARGUMENT) {
            String msg = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
            if (msg.contains("registration token") || msg.contains("registration-token") || msg.contains("token")) {
                return FcmSendResult.invalidToken(deliveryId, detail);
            }
        }
        if (code == MessagingErrorCode.SENDER_ID_MISMATCH) {
            return FcmSendResult.invalidToken(deliveryId, detail);
        }
        return FcmSendResult.retryable(deliveryId, detail);
    }
}
