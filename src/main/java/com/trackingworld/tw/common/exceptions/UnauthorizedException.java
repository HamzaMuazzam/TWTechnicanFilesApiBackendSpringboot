package com.trackingworld.tw.common.exceptions;

import org.springframework.http.HttpStatus;

/** Carries an error-code string that becomes {@code ResponseWrapper.message}. */
public class UnauthorizedException extends BaseException {
    public UnauthorizedException(String errorCode) {
        super(HttpStatus.UNAUTHORIZED, errorCode, errorCode);
    }
}
