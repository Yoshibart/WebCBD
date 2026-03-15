package com.mase.security;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.mase.error.ApiErrorResponseWriter;

// Unit tests for SecurityConfiguration beans.
class SecurityConfigurationTest {

    @Test
    // Verifies encoder can validate its own hashes.
    void passwordEncoder_matchesEncodedValue() {
        SecurityConfiguration config = new SecurityConfiguration(mock(JwtAuthenticationFilter.class),mock(ApplicationUserDetailsService.class),mock(ApiErrorResponseWriter.class));

        PasswordEncoder encoder = config.passwordEncoder();
        String encoded = encoder.encode("secret");

        assertTrue(encoder.matches("secret", encoded));
    }

    @Test
    // Verifies authentication provider type.
    void authenticationProvider_isDaoAuthenticationProvider() {
        SecurityConfiguration config = new SecurityConfiguration(mock(JwtAuthenticationFilter.class),mock(ApplicationUserDetailsService.class),mock(ApiErrorResponseWriter.class));

        AuthenticationProvider provider = config.authenticationProvider();

        assertInstanceOf(DaoAuthenticationProvider.class, provider);
    }
}
