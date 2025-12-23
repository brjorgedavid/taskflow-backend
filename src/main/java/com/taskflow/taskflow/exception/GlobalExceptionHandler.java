package com.taskflow.taskflow.exception;

import com.taskflow.taskflow.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        int status = ex.getStatusCode().value();
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();

        Map<String, Object> data = new HashMap<>();
        data.put("path", request.getRequestURI());

        ApiResponse body = ApiResponse.of(status, message, data);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        int status = 403;
        String message = ex.getMessage() != null ? ex.getMessage() : "Access is denied";
        Map<String, Object> data = Map.of("path", request.getRequestURI());
        ApiResponse body = ApiResponse.of(status, message, data);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        int status = 400;
        String message = "Validation failed";

        Map<String, Object> data = new HashMap<>();
        data.put("path", request.getRequestURI());
        data.put("errors", ex.getBindingResult().getFieldErrors().stream()
                .map(err -> Map.of("field", err.getField(), "message", Objects.toString(err.getDefaultMessage(), "")))
                .toList());

        ApiResponse body = ApiResponse.of(status, message, data);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        int status = 400;
        String message = "Invalid parameter: " + ex.getName();
        Map<String, Object> data = Map.of("path", request.getRequestURI());
        ApiResponse body = ApiResponse.of(status, message, data);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(OverlappingVacationException.class)
    public ResponseEntity<ApiResponse> handleOverlappingVacation(OverlappingVacationException ex, HttpServletRequest request) {
        int status = 400;
        String message = ex.getMessage() != null ? ex.getMessage() : "Requested vacation overlaps with existing bookings";
        Map<String, Object> data = new HashMap<>();
        data.put("path", request.getRequestURI());
        List<Map<String, String>> suggestions = ex.getSuggestions().stream()
                .map(s -> Map.of("startDate", s.getStartDate().toString(), "endDate", s.getEndDate().toString()))
                .toList();
        data.put("suggestions", suggestions);
        data.put("requestedDays", ex.getRequestedDays());
        ApiResponse body = ApiResponse.of(status, message, data);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        int status = 500;
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal server error";
        log.error("Unhandled exception processing request {}", request.getRequestURI(), ex);
        Map<String, Object> data = Map.of("path", request.getRequestURI());
        ApiResponse body = ApiResponse.of(status, message, data);
        return ResponseEntity.status(status).body(body);
    }
}
