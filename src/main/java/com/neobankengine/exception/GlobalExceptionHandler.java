package com.neobankengine.exception;

import com.neobankengine.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        ApiError err = new ApiError(Instant.now(), HttpStatus.NOT_FOUND.value(), "Not Found", ex.getMessage(), req.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        ApiError err = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request", ex.getMessage(), req.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        ApiError err = new ApiError(Instant.now(), HttpStatus.FORBIDDEN.value(), "Forbidden", ex.getMessage(), req.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex, HttpServletRequest req) {
        ApiError err = new ApiError(Instant.now(), HttpStatus.CONFLICT.value(), "Conflict", ex.getMessage(), req.getRequestURI(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    // ✅ NEW: business rule violations (min balance, max 50k, etc.)
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> handleBusinessRule(BusinessRuleException ex, HttpServletRequest req) {
        ApiError err = new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Business Rule Violated",
                ex.getMessage(),           // <-- this is what the UI will show
                req.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    // Handle validation errors from @Valid
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.toList());

        ApiError err = new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), "Validation Failed",
                "One or more validation errors", req.getRequestURI(), details);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    // Catch-all - internal server error
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleAll(Exception ex, HttpServletRequest req) {
        ApiError err = new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",   // generic message
                req.getRequestURI(),
                List.of(ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }
}
