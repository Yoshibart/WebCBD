package com.mase.error;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;

import com.mase.dto.ApiError;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiError> handleResponseStatusException(ResponseStatusException ex) {
        return buildResponse(ex.getStatusCode(), reasonOrDefault(ex.getStatusCode(), ex.getReason()));
    }

    @ExceptionHandler(ErrorResponseException.class)
    public ResponseEntity<ApiError> handleErrorResponseException(ErrorResponseException ex) {
        return buildResponse(ex.getStatusCode(), defaultMessage(ex.getStatusCode()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Invalid request parameter");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Malformed request body");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> {
                    String defaultMessage = error.getDefaultMessage();
                    return (defaultMessage == null || defaultMessage.isBlank())
                            ? error.getField() + " is invalid"
                            : defaultMessage;
                })
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> {
                    String violationMessage = violation.getMessage();
                    return (violationMessage == null || violationMessage.isBlank())
                            ? "Validation failed"
                            : violationMessage;
                })
                .orElse("Validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResourceFound(NoResourceFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Resource not found");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiError> buildResponse(HttpStatusCode statusCode, String message) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return ResponseEntity.status(status).body(new ApiError(toUpperCase(message), status.value()));
    }

    private String reasonOrDefault(HttpStatusCode statusCode, String reason) {
        return (reason == null || reason.isBlank()) ? defaultMessage(statusCode) : reason;
    }

    private String defaultMessage(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> "Bad request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Resource not found";
            case 405 -> "Method not allowed";
            case 409 -> "Conflict";
            default -> "An unexpected error occurred";
        };
    }

    private String toUpperCase(String value) {
        return value.toUpperCase(Locale.ROOT);
    }
}
