package com.mase.service;

import java.util.List;

import com.mase.dto.ProductDto;

public interface ProductService {

    List<ProductDto> getAllProducts();

    ProductDto createProduct(ProductDto productDto);
}
