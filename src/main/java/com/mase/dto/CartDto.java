package com.mase.dto;

import java.util.Set;

public record CartDto(
        String cartId,
        Set<Long> productIds) {
}
