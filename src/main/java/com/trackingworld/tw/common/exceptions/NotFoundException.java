package com.trackingworld.tw.common.exceptions;

import org.springframework.http.HttpStatus;

/** Carries an error-code string that becomes {@code ResponseWrapper.message}. */
public class NotFoundException extends BaseException {
    public NotFoundException(String errorCode) {
        super(HttpStatus.NOT_FOUND, errorCode, errorCode);
    }
}
