package com.taskflow.taskflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private Instant timestamp;
    private int status;
    private String message;
    private Object data;

    public static ApiResponse of(int status, String message, Object data) {
        return new ApiResponse(Instant.now(), status, message, data);
    }

    public static ApiResponse ok(String message, Object data) {
        return of(200, message, data);
    }

    public static ApiResponse ok(String message) {
        return ok(message, null);
    }

    public static ApiResponse error(int status, String message) {
        return of(status, message, null);
    }
}
