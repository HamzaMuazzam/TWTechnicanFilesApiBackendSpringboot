package com.logicaldevs.twlivebackendspringboot.exceptions;

import com.logicaldevs.twlivebackendspringboot.responseparser.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<?> handleBaseException(final BaseException exception) {
        return ResponseEntity.status(exception.getHttpStatus()).body(prepareErrorResponse(exception));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(final Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(prepareErrorResponse(exception));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseWrapper<?>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            errors.put(error.getField(), error.getDefaultMessage());
        });

        return ResponseEntity.badRequest().body(ResponseWrapper.error("Validation failed", errors));
    }

    private ResponseWrapper<?> prepareErrorResponse(final BaseException exception) {
        String errorMessage = exception.getErrorCustomMessage();
        if (errorMessage == null) {
            errorMessage = exception.getMessage();
        }
        return ResponseWrapper.error(errorMessage);
    }

    private ResponseWrapper<?> prepareErrorResponse(final Exception exception) {
        return ResponseWrapper.error(exception.getMessage());
    }
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseWrapper<?>> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        ResponseWrapper<?> errorResponse = ResponseWrapper.error("Endpoint not found", ex);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDeniedException(AccessDeniedException ex) {
        ResponseWrapper<?> errorResponse = ResponseWrapper.error("Access denied - " + ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
    }

}


