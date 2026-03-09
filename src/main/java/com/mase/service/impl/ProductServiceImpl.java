package com.mase.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

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

    private ProductDto toDto(Product product) {
        return new ProductDto(
			product.getName(),
			product.getCategory(),
			product.getPrice(),
			product.getDescription());
    }
}
