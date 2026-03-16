package com.mase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

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

        ResponseEntity<List<ProductDto>> response = controller.getProducts(null, null, null);

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

        ResponseEntity<List<ProductDto>> response = controller.getProducts(0, 5, "id,asc");

        assertEquals(expected.getContent(), response.getBody());
    }

    @Test
    // Verifies paging defaults to id asc when no sort is provided.
    void getProducts_withPaging_usesDefaultSort() {
        when(productService.getProductsPage(any(Pageable.class)))
                .thenReturn(Page.empty());

        controller.getProducts(0, 5, null);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getProductsPage(captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(0, pageable.getPageNumber());
        assertEquals(5, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "id"), pageable.getSort());
    }

    @Test
    // Verifies paging accepts a sort field without direction.
    void getProducts_withPaging_acceptsFieldOnlySort() {
        when(productService.getProductsPage(any(Pageable.class)))
                .thenReturn(Page.empty());

        controller.getProducts(1, 10, "name");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productService).getProductsPage(captor.capture());
        Pageable pageable = captor.getValue();
        assertEquals(1, pageable.getPageNumber());
        assertEquals(10, pageable.getPageSize());
        assertEquals(Sort.by(Sort.Direction.ASC, "name"), pageable.getSort());
    }

    @Test
    // Verifies negative page indexes are rejected.
    void getProducts_withPaging_rejectsNegativePage() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProducts(-1, 5, "id,asc"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    // Verifies invalid page sizes are rejected.
    void getProducts_withPaging_rejectsInvalidPageSize() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProducts(0, 0, "id,asc"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    // Verifies oversized page sizes are rejected.
    void getProducts_withPaging_rejectsOversizedPageSize() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProducts(0, 101, "id,asc"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    // Verifies invalid sort fields are rejected.
    void getProducts_withPaging_rejectsInvalidSortField() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProducts(0, 5, "unknown,asc"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    // Verifies invalid sort directions are rejected.
    void getProducts_withPaging_rejectsInvalidSortDirection() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getProducts(0, 5, "id,sideways"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    // Verifies create product delegates to the service.
    void createProduct_delegatesToService() {
        ProductDto input = new ProductDto(null, "Phone", "Devices", new BigDecimal("199.99"), "Phone");
        ProductDto saved = new ProductDto(2L, "Phone", "Devices", new BigDecimal("199.99"), "Phone");
        when(productService.createProduct(input)).thenReturn(saved);

        ResponseEntity<ProductDto> response = controller.createProduct(input);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(URI.create("/api/ecommerce/v1/products/2"), response.getHeaders().getLocation());
        assertEquals(saved, response.getBody());
    }
}
