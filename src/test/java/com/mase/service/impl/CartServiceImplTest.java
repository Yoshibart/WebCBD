package com.mase.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.mase.dto.CartDto;
import com.mase.model.Cart;
import com.mase.model.Product;
import com.mase.repository.CartRepository;
import com.mase.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
// Unit tests for CartServiceImpl.
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private CartServiceImpl cartService;

    @Test
    // Verifies cart creation returns a DTO with public id.
    void createCart_returnsDto() {
        Cart saved = new Cart();
        saved.setPublicId("cart-1");
        when(cartRepository.save(any(Cart.class))).thenReturn(saved);

        CartDto dto = cartService.createCart();

        assertEquals("cart-1", dto.cartId());
        assertTrue(dto.productIds().isEmpty());
    }

    @Test
    // Verifies cart lookup maps product ids.
    void getCart_mapsProducts() {
        Cart cart = new Cart();
        cart.setPublicId("cart-1");
        Product product = new Product("Tablet", "Electronics", new BigDecimal("399.00"), "Tablet");
        product.setId(7L);
        cart.addProduct(product);
        when(cartRepository.findByPublicId("cart-1")).thenReturn(Optional.of(cart));

        CartDto dto = cartService.getCart("cart-1");

        assertEquals("cart-1", dto.cartId());
        assertEquals(Set.of(7L), dto.productIds());
    }

    @Test
    // Verifies adding a product updates cart contents.
    void addProduct_addsProductToCart() {
        Cart cart = new Cart();
        cart.setPublicId("cart-1");
        Product product = new Product("Chair", "Home", new BigDecimal("49.99"), "Chair");
        product.setId(3L);

        when(cartRepository.findByPublicId("cart-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(3L)).thenReturn(Optional.of(product));
        when(cartRepository.save(cart)).thenReturn(cart);

        CartDto dto = cartService.addProduct("cart-1", 3L);

        assertEquals(Set.of(3L), dto.productIds());
        assertTrue(product.getCarts().contains(cart));
        verify(cartRepository).save(cart);
    }

    @Test
    // Verifies missing cart yields 404.
    void addProduct_rejectsMissingCart() {
        when(cartRepository.findByPublicId("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addProduct("missing", 1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Cart not found", ex.getReason());
    }

    @Test
    // Verifies missing product yields 404.
    void addProduct_rejectsMissingProduct() {
        Cart cart = new Cart();
        cart.setPublicId("cart-1");
        when(cartRepository.findByPublicId("cart-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.addProduct("cart-1", 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Product not found", ex.getReason());
    }

    @Test
    // Verifies removing a product updates cart contents.
    void removeProduct_removesProductFromCart() {
        Cart cart = new Cart();
        cart.setPublicId("cart-1");
        Product product = new Product("Lamp", "Home", new BigDecimal("24.50"), "Lamp");
        product.setId(8L);
        cart.addProduct(product);

        when(cartRepository.findByPublicId("cart-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(8L)).thenReturn(Optional.of(product));
        when(cartRepository.save(cart)).thenReturn(cart);

        CartDto dto = cartService.removeProduct("cart-1", 8L);

        assertTrue(dto.productIds().isEmpty());
        assertFalse(cart.getProducts().contains(product));
        assertFalse(product.getCarts().contains(cart));
        verify(cartRepository).save(cart);
    }

    @Test
    // Verifies missing cart yields 404 on remove.
    void removeProduct_rejectsMissingCart() {
        when(cartRepository.findByPublicId("missing")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.removeProduct("missing", 1L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Cart not found", ex.getReason());
    }

    @Test
    // Verifies missing product yields 404 on remove.
    void removeProduct_rejectsMissingProduct() {
        Cart cart = new Cart();
        cart.setPublicId("cart-1");
        when(cartRepository.findByPublicId("cart-1")).thenReturn(Optional.of(cart));
        when(productRepository.findById(77L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> cartService.removeProduct("cart-1", 77L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Product not found", ex.getReason());
    }

    @Test
    // Verifies list carts returns sorted results.
    void getAllCarts_returnsSortedDtos() {
        Cart cart1 = new Cart();
        cart1.setPublicId("cart-1");
        Cart cart2 = new Cart();
        cart2.setPublicId("cart-2");
        when(cartRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))).thenReturn(List.of(cart1, cart2));

        List<CartDto> result = cartService.getAllCarts();

        assertEquals(2, result.size());
        assertEquals("cart-1", result.get(0).cartId());
        assertEquals("cart-2", result.get(1).cartId());
    }
}
