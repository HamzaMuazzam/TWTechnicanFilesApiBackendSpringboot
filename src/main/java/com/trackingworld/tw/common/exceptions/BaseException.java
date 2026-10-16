package com.trackingworld.tw.common.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String errorCustomMessage;

    public BaseException(HttpStatus httpStatus, String errorCode, String errorCustomMessage) {
        super(errorCustomMessage != null ? errorCustomMessage : errorCode);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorCustomMessage = errorCustomMessage;
    }

    public BaseException(HttpStatus httpStatus, String errorCode) {
        this(httpStatus, errorCode, null);
    }
}
