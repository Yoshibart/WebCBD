package com.mase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

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
    // Verifies list products returns the service result when no paging params are provided.
    void getProducts_withoutPaging_returnsServiceResult() {
        List<ProductDto> expected = List.of(new ProductDto(1L, "Laptop", "Electronics", new BigDecimal("999.99"), "Laptop"));
        when(productService.getAllProducts()).thenReturn(expected);

        ResponseEntity<?> response = controller.getProducts(null, null, null);

        assertEquals(expected, response.getBody());
    }

    @Test
    // Verifies paging params route to the paged service call.
    void getProducts_withPaging_returnsPagedResult() {
        ProductDto dto = new ProductDto(1L, "Laptop", "Electronics", new BigDecimal("999.99"), "Laptop");
        Page<ProductDto> expected = new PageImpl<>(
                List.of(dto),
                PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "id")),
                1);
        when(productService.getProductsPage(any(Pageable.class))).thenReturn(expected);

        ResponseEntity<?> response = controller.getProducts(0, 5, "id,asc");

        assertEquals(expected, response.getBody());
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
