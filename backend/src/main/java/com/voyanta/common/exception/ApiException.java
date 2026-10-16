package com.voyanta.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Base Exception for all expected business errors in Voyanta.
 * It carries the HTTP status and a short error code (for example "RESOURCE_NOT_FOUND")
 * so that GlobalExceptionHandler can turn it into a proper API error response.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }
}