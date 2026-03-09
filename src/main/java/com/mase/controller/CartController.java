package com.mase.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mase.dto.CartDto;
import com.mase.service.CartService;

@RestController
@RequestMapping("/api/ecommerce/v1/carts")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping
    public CartDto createCart() {
        return cartService.createCart();
    }

    @GetMapping("/{cartId}")
    public CartDto getCart(@PathVariable String cartId) {
        return cartService.getCart(cartId);
    }

    @PostMapping("/{cartId}/products/{productId}")
    public CartDto addProduct(@PathVariable String cartId, @PathVariable Long productId) {
        return cartService.addProduct(cartId, productId);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<CartDto> getAllCarts() {
        return cartService.getAllCarts();
    }
}
