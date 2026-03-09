package com.mase.dto;

import java.util.Set;

public record CartDto(
        Long id,
        Long userId,
        Set<Long> productIds) {
}
