package com.mase.dto;

import java.math.BigDecimal;

public record ProductDto(
	String name,
	String category,
	BigDecimal price,
	String description
)
{}
