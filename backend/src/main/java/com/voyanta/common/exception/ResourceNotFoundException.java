package com.voyanta.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception thrown when a requested record does not exist, such as a missing plan,
 * user or expired survey session. Returns HTTP 404 with the error code "RESOURCE_NOT_FOUND".
 */
public class ResourceNotFoundException extends ApiException {
    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }
}