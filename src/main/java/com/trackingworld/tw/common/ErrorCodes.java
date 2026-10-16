package com.trackingworld.tw.common;

/** Error code strings returned in {@code ResponseWrapper.message} when {@code success=false}. */
public final class ErrorCodes {

    private ErrorCodes() {
    }

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_REQUEST = "MALFORMED_REQUEST";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String INVALID_TOKEN = "INVALID_TOKEN";
    public static final String TOKEN_EXPIRED = "TOKEN_EXPIRED";
    public static final String INVALID_API_KEY = "INVALID_API_KEY";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String INVALID_REFRESH_TOKEN = "INVALID_REFRESH_TOKEN";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String DUPLICATE = "DUPLICATE";
    public static final String RATE_LIMITED = "RATE_LIMITED";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String FILE_TOO_LARGE = "FILE_TOO_LARGE";
    public static final String UNKNOWN_FIELD = "UNKNOWN_FIELD";
    public static final String UNKNOWN_ENTITY = "UNKNOWN_ENTITY";
    public static final String UNKNOWN_OPERATOR = "UNKNOWN_OPERATOR";
    public static final String INVALID_FILTER_VALUE = "INVALID_FILTER_VALUE";
    public static final String INVALID_PHONE = "INVALID_PHONE";
    public static final String PHONE_NOT_REGISTERED = "PHONE_NOT_REGISTERED";
    public static final String USER_DISABLED = "USER_DISABLED";
    public static final String DEVICE_NOT_FOUND = "DEVICE_NOT_FOUND";
    public static final String DEV_LOGIN_DISABLED = "DEV_LOGIN_DISABLED";
    public static final String FIREBASE_DISABLED = "FIREBASE_DISABLED";
    public static final String FIREBASE_TOKEN_INVALID = "FIREBASE_TOKEN_INVALID";
    public static final String FIREBASE_PHONE_MISSING = "FIREBASE_PHONE_MISSING";
    public static final String BATCH_TOO_LARGE = "BATCH_TOO_LARGE";
    public static final String EMPTY_BATCH = "EMPTY_BATCH";
    public static final String ALERT_TYPE_IN_USE = "ALERT_TYPE_IN_USE";
    public static final String ALERT_TYPE_EXISTS = "ALERT_TYPE_EXISTS";
    public static final String SOURCE_EXISTS = "SOURCE_EXISTS";
    public static final String ADMIN_EXISTS = "ADMIN_EXISTS";
    public static final String CANNOT_DELETE_SELF = "CANNOT_DELETE_SELF";
    public static final String LAST_ADMIN = "LAST_ADMIN";
    public static final String INVALID_SETTING = "INVALID_SETTING";
    public static final String SQL_NOT_ALLOWED = "SQL_NOT_ALLOWED";
    public static final String SQL_ERROR = "SQL_ERROR";
}
