package com.mase.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mase.model.RevokedToken;
import com.mase.repository.RevokedTokenRepository;
import com.mase.security.JwtService;

@ExtendWith(MockitoExtension.class)
// Unit tests for TokenRevocationServiceImpl.
class TokenRevocationServiceImplTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenRevocationCleanupService cleanupService;

    @InjectMocks
    private TokenRevocationServiceImpl service;

    @Test
    // Verifies new tokens are hashed and saved.
    void revoke_savesNewToken() {
        String token = "token-123";
        String tokenHash = hash(token);
        Instant expiresAt = Instant.parse("2030-01-01T00:00:00Z");

        when(revokedTokenRepository.existsByTokenHash(tokenHash)).thenReturn(false);
        when(jwtService.extractExpiration(token)).thenReturn(Date.from(expiresAt));

        service.revoke(token);

        verify(cleanupService).deleteExpiredTokens();
        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        RevokedToken saved = captor.getValue();
        assertEquals(tokenHash, saved.getTokenHash());
        assertEquals(expiresAt, saved.getExpiresAt());
    }

    @Test
    // Verifies existing tokens are not saved twice.
    void revoke_skipsWhenAlreadyRevoked() {
        String token = "token-123";
        String tokenHash = hash(token);
        when(revokedTokenRepository.existsByTokenHash(tokenHash)).thenReturn(true);

        service.revoke(token);

        verify(cleanupService).deleteExpiredTokens();
        verify(revokedTokenRepository, never()).save(any(RevokedToken.class));
    }

    @Test
    // Verifies revocation checks use hashed token.
    void isRevoked_usesHashedToken() {
        String token = "token-xyz";
        String tokenHash = hash(token);
        when(revokedTokenRepository.existsByTokenHash(tokenHash)).thenReturn(true);

        assertTrue(service.isRevoked(token));

        verifyNoMoreInteractions(cleanupService);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
