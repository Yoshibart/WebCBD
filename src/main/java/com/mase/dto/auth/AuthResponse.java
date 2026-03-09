package com.mase.dto.auth;

import com.mase.model.Role;

public record AuthResponse(
	String accessToken,
	String tokenType,
	long expiresIn,
	String username,
	String email,
	Role role)
{}