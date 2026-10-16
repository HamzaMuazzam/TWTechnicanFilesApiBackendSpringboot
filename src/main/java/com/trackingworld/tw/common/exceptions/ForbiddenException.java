package com.trackingworld.tw.common.exceptions;

import org.springframework.http.HttpStatus;

/** Carries an error-code string that becomes {@code ResponseWrapper.message}. */
public class ForbiddenException extends BaseException {
    public ForbiddenException(String errorCode) {
        super(HttpStatus.FORBIDDEN, errorCode, errorCode);
    }
}
