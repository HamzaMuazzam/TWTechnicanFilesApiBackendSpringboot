package com.logicaldevs.twlivebackendspringboot.constants;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ErrorCodeConstant {
    public final String BAD_REQUEST_ERROR_CODE = "0001";
    public final String ACCOUNT_NOT_FOUND_ERROR_CODE = "0002";
    public final String INVALID_OTP_ERROR_CODE = "0003";
    public final String NOT_FOUND_ERROR_CODE = "004";
    public final String PACKAGE_EXPIRED_CODE = "005";
    public final String AUTHENTICATION_FAILED_ERROR_CODE = "0006";
}
