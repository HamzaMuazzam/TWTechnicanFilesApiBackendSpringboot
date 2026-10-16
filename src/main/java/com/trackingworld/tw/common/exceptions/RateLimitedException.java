package com.trackingworld.tw.common.exceptions;

import org.springframework.http.HttpStatus;

/** Carries an error-code string that becomes {@code ResponseWrapper.message}. */
public class RateLimitedException extends BaseException {
    public RateLimitedException(String errorCode) {
        super(HttpStatus.TOO_MANY_REQUESTS, errorCode, errorCode);
    }
}
