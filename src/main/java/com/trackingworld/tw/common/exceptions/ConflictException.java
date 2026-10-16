package com.trackingworld.tw.common.exceptions;

import org.springframework.http.HttpStatus;

/** Carries an error-code string that becomes {@code ResponseWrapper.message}. */
public class ConflictException extends BaseException {
    public ConflictException(String errorCode) {
        super(HttpStatus.CONFLICT, errorCode, errorCode);
    }
}
