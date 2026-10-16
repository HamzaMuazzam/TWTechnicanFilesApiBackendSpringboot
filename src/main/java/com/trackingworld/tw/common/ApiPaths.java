package com.trackingworld.tw.common;

/**
 * Api end points
 * Do not use hard coded end points in the controllers
 * instead define end points here and use them in controllers.
 * Created At: Thursday, 19 March 2025 at 11:16 PM
 * HAMZA MUAZZAM
 * hamzamuazzam@gmail.com
 **/

public final class ApiPaths {

    private ApiPaths() {
    }

    // ---- legacy files API (public URLs must stay byte-identical) ----
    public static final String TECHNICIAN_BASE = "/technician/v1";
    public static final String UPLOAD_FILE = "/upload-file";
    public static final String FILE_PUBLIC_PATH = "/file-public-path";

    // ---- notification platform ----
    public static final String API_V1 = "/api/v1";
    public static final String INGEST = API_V1 + "/ingest";
    public static final String APP = API_V1 + "/app";
    public static final String APP_AUTH = APP + "/auth";
    public static final String APP_DEVICES = APP + "/devices";
    public static final String APP_NOTIFICATIONS = APP + "/notifications";
    public static final String APP_ALERT_TYPES = APP + "/alert-types";
    public static final String ADMIN = API_V1 + "/admin";
    public static final String ADMIN_AUTH = ADMIN + "/auth";
    public static final String ADMIN_SPA = "/admin";
}
