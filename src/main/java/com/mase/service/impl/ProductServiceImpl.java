package com.mase.service.impl;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mase.dto.ProductDto;
import com.mase.model.Product;
import com.mase.repository.ProductRepository;
import com.mase.service.ProductService;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<ProductDto> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public ProductDto createProduct(ProductDto productDto) {
        Product product = new Product(
                requireText(productDto.name(), "Name"),
                requireText(productDto.category(), "Category"),
                requirePrice(productDto.price()),
                requireText(productDto.description(), "Description"));

        return toDto(productRepository.save(product));
    }

    private ProductDto toDto(Product product) {
        return new ProductDto(
			product.getId(),
			product.getName(),
			product.getCategory(),
			product.getPrice(),
			product.getDescription());
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " is required");
        }
        return value.trim();
    }

    private java.math.BigDecimal requirePrice(java.math.BigDecimal value) {
        if (value == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required");
        }
        return value;
    }
}
