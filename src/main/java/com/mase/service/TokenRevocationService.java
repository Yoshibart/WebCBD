package com.mase.service;

public interface TokenRevocationService {

    void revoke(String token);

    boolean isRevoked(String token);
}
