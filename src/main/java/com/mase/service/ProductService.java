package com.mase.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.mase.dto.ProductDto;

public interface ProductService {

    Page<ProductDto> getProductsPage(Pageable pageable);

    List<ProductDto> getAllProducts();

    ProductDto createProduct(ProductDto productDto);
}
