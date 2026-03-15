package com.mase.service.impl;

import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mase.repository.RevokedTokenRepository;

@Service
public class TokenRevocationCleanupService {

    private final RevokedTokenRepository revokedTokenRepository;

    public TokenRevocationCleanupService(RevokedTokenRepository revokedTokenRepository) {
        this.revokedTokenRepository = revokedTokenRepository;
    }

    @Transactional
    public void deleteExpiredTokens() {
        revokedTokenRepository.deleteByExpiresAtBefore(Instant.now());
    }
}
