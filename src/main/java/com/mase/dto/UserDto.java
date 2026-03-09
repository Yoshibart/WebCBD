package com.mase.dto;

import java.util.List;

public record UserDto(
        Long id,
        String username,
        String email,
        List<Long> cartIds) {
}
