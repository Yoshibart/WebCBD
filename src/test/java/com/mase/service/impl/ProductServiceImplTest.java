package com.mase.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mase.dto.ProductDto;
import com.mase.model.Product;
import com.mase.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
// Unit tests for ProductServiceImpl.
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    // Verifies products are mapped to DTOs.
    void getAllProducts_mapsEntitiesToDtos() {
        Product product = new Product("Laptop", "Electronics", new BigDecimal("999.99"), "Powerful laptop");
        product.setId(10L);
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductDto> result = productService.getAllProducts();

        assertEquals(1, result.size());
        ProductDto dto = result.get(0);
        assertEquals(10L, dto.id());
        assertEquals("Laptop", dto.name());
        assertEquals("Electronics", dto.category());
        assertEquals(new BigDecimal("999.99"), dto.price());
        assertEquals("Powerful laptop", dto.description());
    }

    @Test
    // Verifies input is trimmed and persisted.
    void createProduct_trimsValuesAndPersists() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product saved = invocation.getArgument(0);
            saved.setId(42L);
            return saved;
        });

        ProductDto input = new ProductDto(
                null,
                "  Phone  ",
                "  Devices ",
                new BigDecimal("199.99"),
                "  Smart phone  ");

        ProductDto result = productService.createProduct(input);

        assertEquals(42L, result.id());
        assertEquals("Phone", result.name());
        assertEquals("Devices", result.category());
        assertEquals(new BigDecimal("199.99"), result.price());
        assertEquals("Smart phone", result.description());

        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        Product saved = captor.getValue();
        assertEquals("Phone", saved.getName());
        assertEquals("Devices", saved.getCategory());
        assertEquals(new BigDecimal("199.99"), saved.getPrice());
        assertEquals("Smart phone", saved.getDescription());
    }

    @Test
    // Verifies missing name yields 400.
    void createProduct_rejectsMissingName() {
        ProductDto input = new ProductDto(null, "  ", "Category", new BigDecimal("9.99"), "Desc");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(input));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Name is required", ex.getReason());
    }

    @Test
    // Verifies missing price yields 400.
    void createProduct_rejectsMissingPrice() {
        ProductDto input = new ProductDto(null, "Name", "Category", null, "Desc");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(input));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Price is required", ex.getReason());
    }

    @Test
    // Verifies missing category yields 400.
    void createProduct_rejectsMissingCategory() {
        ProductDto input = new ProductDto(null, "Name", " ", new BigDecimal("9.99"), "Desc");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> productService.createProduct(input));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Category is required", ex.getReason());
    }
}
