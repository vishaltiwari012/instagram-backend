package com.instagram.backend.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@AllArgsConstructor
public class ApiError {
    private final String message;
    private final HttpStatus status;
    private final LocalDateTime timestamp;
    private final String path;
    private final ErrorCode errorCode;
    private final Map<String, String> validationErrors;

    // Full constructor with all fields
    public ApiError(String message, HttpStatus status, String path, ErrorCode errorCode, Map<String, String> validationErrors) {
        this.message = message;
        this.status = status;
        this.timestamp = LocalDateTime.now();
        this.path = path;
        this.errorCode = errorCode;
        this.validationErrors = validationErrors;
    }

    // Constructor without validation errors
    public ApiError(String message, HttpStatus status, String path, ErrorCode errorCode) {
        this(message, status, path, errorCode, null);
    }
}