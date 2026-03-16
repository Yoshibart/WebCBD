package com.mase.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import jakarta.validation.ConstraintViolationException;

import com.mase.dto.ApiError;

// Unit tests for GlobalExceptionHandler status and message mapping.
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    // Verifies default reason mapping for ResponseStatusException.
    void handleResponseStatusException_usesReasonOrDefault() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST);
        ResponseEntity<ApiError> response = handler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("BAD REQUEST", response.getBody().message());
        assertEquals(400, response.getBody().code());
    }

    @Test
    // Verifies provided reason overrides default message.
    void handleResponseStatusException_usesProvidedReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "Name is required");
        ResponseEntity<ApiError> response = handler.handleResponseStatusException(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("NAME IS REQUIRED", response.getBody().message());
        assertEquals(409, response.getBody().code());
    }

    @Test
    // Verifies ErrorResponseException uses standard message mapping.
    void handleErrorResponseException_usesDefaultMessage() {
        ErrorResponseException ex = new ErrorResponseException(HttpStatus.NOT_FOUND);
        ResponseEntity<ApiError> response = handler.handleErrorResponseException(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE NOT FOUND", response.getBody().message());
        assertEquals(404, response.getBody().code());
    }

    @Test
    // Verifies bad parameter mismatch yields 400 response.
    void handleMethodArgumentTypeMismatch_returnsBadRequest() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException("bad",String.class,"id",null,null);
        ResponseEntity<ApiError> response = handler.handleMethodArgumentTypeMismatch(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("INVALID REQUEST PARAMETER", response.getBody().message());
        assertEquals(400, response.getBody().code());
    }

    @Test
    // Verifies malformed JSON yields 400 response.
    void handleHttpMessageNotReadable_returnsBadRequest() {
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException("bad",new MockHttpInputMessage(new byte[0]));
        ResponseEntity<ApiError> response = handler.handleHttpMessageNotReadable(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("MALFORMED REQUEST BODY", response.getBody().message());
        assertEquals(400, response.getBody().code());
    }

    @Test
    // Verifies bean validation errors yield 400 response.
    void handleMethodArgumentNotValid_returnsBadRequest() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "name", "Name is required"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        ResponseEntity<ApiError> response = handler.handleMethodArgumentNotValid(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("NAME IS REQUIRED", response.getBody().message());
        assertEquals(400, response.getBody().code());
    }

    @Test
    // Verifies constraint violations yield 400 response.
    void handleConstraintViolation_returnsBadRequest() {
        ConstraintViolationException ex = new ConstraintViolationException(Collections.emptySet());
        ResponseEntity<ApiError> response = handler.handleConstraintViolation(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("VALIDATION FAILED", response.getBody().message());
        assertEquals(400, response.getBody().code());
    }

    @Test
    // Verifies unsupported method yields 405 response.
    void handleMethodNotSupported_returnsMethodNotAllowed() {
        HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("PUT");
        ResponseEntity<ApiError> response = handler.handleMethodNotSupported(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertEquals("METHOD NOT ALLOWED", response.getBody().message());
        assertEquals(405, response.getBody().code());
    }

    @Test
    // Verifies missing resource yields 404 response.
    void handleNoResourceFound_returnsNotFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "/missing", "missing");
        ResponseEntity<ApiError> response = handler.handleNoResourceFound(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("RESOURCE NOT FOUND", response.getBody().message());
        assertEquals(404, response.getBody().code());
    }

    @Test
    // Verifies unexpected errors yield 500 response.
    void handleUnexpectedException_returnsInternalServerError() {
        ResponseEntity<ApiError> response = handler.handleUnexpectedException(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("AN UNEXPECTED ERROR OCCURRED", response.getBody().message());
        assertEquals(500, response.getBody().code());
    }
}
