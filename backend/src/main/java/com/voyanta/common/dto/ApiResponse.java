package com.voyanta.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Generic response DTO used as the envelope for every Voyanta API response.
 * It wraps the payload together with a success flag and, on failure, an error
 * message and error code. Use ok(...) for success and error(...) for failures.
 */
@Getter
@Builder
@AllArgsConstructor
public class ApiResponse<T> {

    private boolean success;
    private T data;
    private String error;
    private String errorCode;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> error(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorCode(errorCode)
                .error(message)
                .build();
    }
}