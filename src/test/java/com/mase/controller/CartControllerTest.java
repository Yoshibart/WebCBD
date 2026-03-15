package com.mase.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mase.dto.CartDto;
import com.mase.service.CartService;

@ExtendWith(MockitoExtension.class)
// Unit tests for CartController request delegation.
class CartControllerTest {

    @Mock
    private CartService cartService;

    @InjectMocks
    private CartController controller;

    @Test
    // Verifies cart creation delegates to the service.
    void createCart_delegatesToService() {
        CartDto expected = new CartDto("cart-1", Set.of());
        when(cartService.createCart()).thenReturn(expected);

        CartDto result = controller.createCart();

        assertEquals(expected, result);
    }

    @Test
    // Verifies cart lookup delegates to the service.
    void getCart_delegatesToService() {
        CartDto expected = new CartDto("cart-2", Set.of(1L));
        when(cartService.getCart("cart-2")).thenReturn(expected);

        CartDto result = controller.getCart("cart-2");

        assertEquals(expected, result);
    }

    @Test
    // Verifies add-product delegates to the service.
    void addProduct_delegatesToService() {
        CartDto expected = new CartDto("cart-3", Set.of(2L));
        when(cartService.addProduct("cart-3", 2L)).thenReturn(expected);

        CartDto result = controller.addProduct("cart-3", 2L);

        assertEquals(expected, result);
    }

    @Test
    // Verifies list carts delegates to the service.
    void getAllCarts_delegatesToService() {
        List<CartDto> expected = List.of(new CartDto("cart-1", Set.of()));
        when(cartService.getAllCarts()).thenReturn(expected);

        List<CartDto> result = controller.getAllCarts();

        assertEquals(expected, result);
    }
}
