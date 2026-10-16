package com.trackingworld.tw.common.exceptions;

import org.springframework.http.HttpStatus;

/** Carries an error-code string that becomes {@code ResponseWrapper.message}. */
public class ServiceUnavailableException extends BaseException {
    public ServiceUnavailableException(String errorCode) {
        super(HttpStatus.SERVICE_UNAVAILABLE, errorCode, errorCode);
    }
}
