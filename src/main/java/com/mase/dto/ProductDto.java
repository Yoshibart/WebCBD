package com.mase.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductDto(
	Long id,
	@NotBlank(message = "Name is required")
	@Size(max = 150, message = "Name must be at most 150 characters")
	String name,
	@NotBlank(message = "Category is required")
	@Size(max = 100, message = "Category must be at most 100 characters")
	String category,
	@NotNull(message = "Price is required")
	@DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
	BigDecimal price,
	@NotBlank(message = "Description is required")
	@Size(max = 1000, message = "Description must be at most 1000 characters")
	String description
)
{}
