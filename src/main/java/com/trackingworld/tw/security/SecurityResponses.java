package com.trackingworld.tw.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.common.ResponseWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes ResponseWrapper error bodies from filters / entry points (outside @ControllerAdvice). */
public final class SecurityResponses {

    private SecurityResponses() {
    }

    public static void write(HttpServletResponse response, ObjectMapper mapper, int status, String errorCode) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        mapper.writeValue(response.getWriter(), ResponseWrapper.error(errorCode));
    }
}
