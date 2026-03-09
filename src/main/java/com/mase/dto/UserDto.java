package com.mase.dto;

import java.util.List;

import com.mase.model.Role;

public record UserDto(
        Long id,
        String username,
        String email,
        Role role,
        List<Long> cartIds) {
}
