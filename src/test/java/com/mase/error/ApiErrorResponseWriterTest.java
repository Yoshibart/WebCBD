package com.mase.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

// Unit tests for ApiErrorResponseWriter JSON output.
class ApiErrorResponseWriterTest {

    @Test
    // Verifies status, content type, and JSON escaping.
    void write_setsStatusContentTypeAndEscapesJson() throws IOException {
        ApiErrorResponseWriter writer = new ApiErrorResponseWriter();
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, HttpStatus.UNAUTHORIZED, "Bad \"token\" \\ invalid");

        assertEquals(HttpStatus.UNAUTHORIZED.value(), response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_VALUE, response.getContentType());
        assertEquals("{\"message\":\"BAD \\\"TOKEN\\\" \\\\ INVALID\",\"code\":401}", response.getContentAsString());
    }
}
