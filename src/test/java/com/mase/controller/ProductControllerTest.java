package com.mase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mase.dto.ProductDto;
import com.mase.service.ProductService;

@ExtendWith(MockitoExtension.class)
// Unit tests for ProductController request delegation.
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    @Test
    // Verifies list products returns the service result.
    void getAllProducts_returnsServiceResult() {
        List<ProductDto> expected = List.of(new ProductDto(1L, "Laptop", "Electronics", new BigDecimal("999.99"), "Laptop"));
        when(productService.getAllProducts()).thenReturn(expected);

        List<ProductDto> result = controller.getAllProducts();

        assertEquals(expected, result);
    }

    @Test
    // Verifies create product delegates to the service.
    void createProduct_delegatesToService() {
        ProductDto input = new ProductDto(null, "Phone", "Devices", new BigDecimal("199.99"), "Phone");
        ProductDto saved = new ProductDto(2L, "Phone", "Devices", new BigDecimal("199.99"), "Phone");
        when(productService.createProduct(input)).thenReturn(saved);

        ProductDto result = controller.createProduct(input);

        assertEquals(saved, result);
    }
}
