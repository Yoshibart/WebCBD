package com.mase.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import com.mase.model.Role;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Encoders;

// Unit tests for JwtService token behavior.
class JwtServiceTest {

    @Test
    // Verifies token creation and claim extraction.
    void generateToken_and_extractUsername() {
        String secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        JwtService jwtService = new JwtService(secret, 5_000L);

        com.mase.model.User user = new com.mase.model.User("admin", "admin@example.com", "pass", Role.ADMIN);
        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertEquals("admin", jwtService.extractUsername(token));
        assertTrue(jwtService.extractExpiration(token).after(Date.from(Instant.now())));
    }

    @Test
    // Verifies a valid token matches the expected user.
    void isTokenValid_returnsTrueForMatchingUser() {
        String secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        JwtService jwtService = new JwtService(secret, 5_000L);

        com.mase.model.User user = new com.mase.model.User("admin", "admin@example.com", "pass", Role.ADMIN);
        String token = jwtService.generateToken(user);

        UserDetails userDetails = new User(
                "admin",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    // Verifies expired tokens are treated as invalid.
    void isTokenValid_returnsFalseForExpiredToken() {
        String secret = Encoders.BASE64.encode(Jwts.SIG.HS256.key().build().getEncoded());
        JwtService jwtService = new JwtService(secret, -1_000L);

        com.mase.model.User user = new com.mase.model.User("admin", "admin@example.com", "pass", Role.ADMIN);
        String token = jwtService.generateToken(user);

        UserDetails userDetails = new User(
                "admin",
                "pass",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        assertFalse(jwtService.isTokenValid(token, userDetails));
    }
}
