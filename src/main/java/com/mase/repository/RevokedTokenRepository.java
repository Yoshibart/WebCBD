package com.mase.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mase.model.RevokedToken;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByTokenHash(String tokenHash);

    void deleteByExpiresAtBefore(Instant instant);
}
