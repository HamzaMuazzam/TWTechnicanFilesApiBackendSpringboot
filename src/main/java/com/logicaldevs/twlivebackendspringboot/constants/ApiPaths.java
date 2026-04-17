package com.logicaldevs.twlivebackendspringboot.constants;

/**
 * Api end points
 * Do not use hard coded end points in the controllers
 * instead define end points here and use them in controllers.
 * Created At: Thursday, 19 March 2025 at 11:16 PM
 * HAMZA MUAZZAM
 * hamzamuazzam@gmail.com
 **/

public class ApiPaths {

    public static final String API_VERSION = "/v1";

    public static final String BASE_API = API_VERSION;

    public static final String authentication = BASE_API + "/authentication";
    public static final String UPLOAD_FILE = "/upload-file";
    public static final String FILE_PUBLIC_PATH = "/file-public-path";
}
