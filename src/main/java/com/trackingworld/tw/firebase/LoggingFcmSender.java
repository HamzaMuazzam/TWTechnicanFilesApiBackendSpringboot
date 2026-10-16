package com.trackingworld.tw.firebase;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/** Dev sender (firebase.enabled=false): logs the payload and reports success with message id "DEV". */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingFcmSender implements FcmSender {

    public static final String DEV_MESSAGE_ID = "DEV";

    @Override
    public List<FcmSendResult> sendEach(List<FcmMessage> messages) {
        return messages.stream().map(m -> {
            log.info("[FCM][DEV] would send to token={}... data={}", abbreviate(m.token()), m.data());
            return FcmSendResult.ok(m.deliveryId(), DEV_MESSAGE_ID);
        }).toList();
    }

    private static String abbreviate(String token) {
        if (token == null) {
            return "null";
        }
        return token.length() <= 12 ? token : token.substring(0, 12);
    }
}
