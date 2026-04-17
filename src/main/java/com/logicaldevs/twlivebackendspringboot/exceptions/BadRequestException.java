
package com.logicaldevs.twlivebackendspringboot.exceptions;

import com.logicaldevs.twlivebackendspringboot.constants.ErrorCodeConstant;
import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseException {
    public BadRequestException() {
        super(HttpStatus.BAD_REQUEST, ErrorCodeConstant.BAD_REQUEST_ERROR_CODE);
    }

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCodeConstant.BAD_REQUEST_ERROR_CODE, message);
    }
}
