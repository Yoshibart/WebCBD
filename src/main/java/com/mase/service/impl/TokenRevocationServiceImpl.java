package com.mase.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mase.model.RevokedToken;
import com.mase.repository.RevokedTokenRepository;
import com.mase.security.JwtService;
import com.mase.service.TokenRevocationService;

@Service
public class TokenRevocationServiceImpl implements TokenRevocationService {

    private final RevokedTokenRepository revokedTokenRepository;
    private final JwtService jwtService;
    private final TokenRevocationCleanupService cleanupService;

    public TokenRevocationServiceImpl(
            RevokedTokenRepository revokedTokenRepository,
            JwtService jwtService,
            TokenRevocationCleanupService cleanupService) {
        this.revokedTokenRepository = revokedTokenRepository;
        this.jwtService = jwtService;
        this.cleanupService = cleanupService;
    }

    @Override
    @Transactional
    public void revoke(String token) {
        cleanupService.deleteExpiredTokens();

        String tokenHash = hash(token);
        if (revokedTokenRepository.existsByTokenHash(tokenHash)) {
            return;
        }

        revokedTokenRepository.save(new RevokedToken(tokenHash, jwtService.extractExpiration(token).toInstant()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isRevoked(String token) {
        return revokedTokenRepository.existsByTokenHash(hash(token));
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
