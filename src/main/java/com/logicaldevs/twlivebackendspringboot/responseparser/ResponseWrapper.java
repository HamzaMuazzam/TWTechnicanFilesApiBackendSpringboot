package com.logicaldevs.twlivebackendspringboot.responseparser;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;


@Setter
@Getter
@Slf4j
public class ResponseWrapper<T> {
    private boolean success;
    private String message;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    private T data;

    public ResponseWrapper(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    public static <T> ResponseWrapper<T> success(T data) {
        return new ResponseWrapper<>(true, "Operation successful", data);
    }

    public static <T> ResponseWrapper<T> success(boolean status ,String message, T data) {
        return new ResponseWrapper<>(status, message, data);
    }

    public static <T> ResponseWrapper<T> success(String message, T data) {
        return new ResponseWrapper<>(true, message, data);
    }

    public static <T> ResponseWrapper<T> error(String message, T data) {
        return new ResponseWrapper<>(false, message, data);
    }

    public static <T> ResponseWrapper<T> error(String message) {
        return new ResponseWrapper<>(false, message, null);
    }

    @Override
    public String toString() {
        return "ResponseWrapper{" +
                "success=" + success +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }

    public String toJsonString() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // Register JavaTimeModule for LocalDateTime

        try {
            return objectMapper.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            log.error("Error converting object to JSON string: {}", e.getMessage());
            return "{}"; // Return an empty JSON object in case of error
        }
    }
}
