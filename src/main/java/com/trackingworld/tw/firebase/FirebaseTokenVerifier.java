package com.trackingworld.tw.firebase;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.exceptions.ServiceUnavailableException;
import com.trackingworld.tw.common.exceptions.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Verifies Firebase ID tokens (phone auth) and returns the phone_number claim. */
@Slf4j
@Component
public class FirebaseTokenVerifier {

    private final ObjectProvider<FirebaseApp> firebaseApp;

    public FirebaseTokenVerifier(ObjectProvider<FirebaseApp> firebaseApp) {
        this.firebaseApp = firebaseApp;
    }

    public boolean isEnabled() {
        return firebaseApp.getIfAvailable() != null;
    }

    /** @return the verified phone number exactly as Firebase reports it (E.164). */
    public String verifyPhone(String idToken) {
        FirebaseApp app = firebaseApp.getIfAvailable();
        if (app == null) {
            throw new ServiceUnavailableException(ErrorCodes.FIREBASE_DISABLED);
        }
        if (idToken == null || idToken.isBlank()) {
            throw new UnauthorizedException(ErrorCodes.FIREBASE_TOKEN_INVALID);
        }
        FirebaseToken token;
        try {
            token = FirebaseAuth.getInstance(app).verifyIdToken(idToken.trim());
        } catch (FirebaseAuthException e) {
            log.warn("[AUTH][FIREBASE] token rejected: {} {}", e.getAuthErrorCode(), e.getMessage());
            throw new UnauthorizedException(ErrorCodes.FIREBASE_TOKEN_INVALID);
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException(ErrorCodes.FIREBASE_TOKEN_INVALID);
        }
        Object phone = token.getClaims().get("phone_number");
        if (phone == null || phone.toString().isBlank()) {
            log.warn("[AUTH][FIREBASE] token for uid={} has no phone_number claim", token.getUid());
            throw new UnauthorizedException(ErrorCodes.FIREBASE_PHONE_MISSING);
        }
        return phone.toString();
    }
}
