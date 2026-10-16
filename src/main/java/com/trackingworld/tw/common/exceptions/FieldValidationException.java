package com.trackingworld.tw.common.exceptions;

import com.trackingworld.tw.common.ErrorCodes;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.Map;

/** 400 VALIDATION_ERROR with a field -> message map in {@code data} (for manually validated payloads). */
@Getter
public class FieldValidationException extends BaseException {

    private final transient Map<String, String> errors;

    public FieldValidationException(Map<String, String> errors) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, ErrorCodes.VALIDATION_ERROR);
        this.errors = errors;
    }
}
